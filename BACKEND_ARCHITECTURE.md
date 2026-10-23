# GOLDEN SQUARE Backend Architecture & Firebase Integration Guide

This document specifies the backend security architecture, Firestore data schema, Cloud Functions specifications, and deployment steps for **GOLDEN SQUARE** (Free Fire esports scrims and custom room hosting platform).

---

## 1. Zero-Trust Security Model

In accordance with strict esports platform guidelines:
1. **Never Trust Client Balance Updates:** Gold Points cannot be written directly by client apps. All balance-changing actions occur inside atomic transactions executed by Google Cloud Functions using the Firebase Admin SDK.
2. **Promotional Platform Points Only:** Gold Points are promotional non-cash platform points. They carry no cash redemption or trade value.
3. **Atomic 50 Gold Points Slot Creation Fee:**
   - Server reads the team document within a transaction.
   - Verifies `team.goldPoints >= 50`.
   - Decrements `team.goldPoints` by 50.
   - Creates the `customSlots/{slotId}` document.
   - Writes an immutable record to `transactions/{txId}` with `type: 'SLOT_CREATION_FEE'`, `amount: -50`, `idempotencyKey`, and `relatedSlotId`.
4. **Monthly 1,000 Gold Points Team Reward Safeguard:**
   - Enforces 1 claim per registered squad per calendar month (`YYYY-MM`).
   - Transaction checks `team.monthlyRewardLastClaimedMonth != currentMonthKey`.
   - Prevents duplicate claims across retries using idempotency tokens.
5. **Private Custom Room Credentials:**
   - Free Fire Room ID and Password are stored in a protected subcollection (`privateCredentials`) or gated document.
   - Read permissions are granted **only** to the host squad captain, approved participant squads, and platform administrators.

---

## 2. Cloud Firestore Data Schema

### `users/{userId}`
```typescript
interface UserDocument {
  id: string;               // Firebase Auth UID
  username: string;         // Account handle
  ign: string;              // Free Fire In-Game Name
  ffUid: string;            // Free Fire UID
  email: string;
  role: 'ORDINARY_MEMBER' | 'TEAM_CAPTAIN' | 'ADMIN';
  teamId: string | null;    // Reference to team
  createdAt: number;
}
```

### `teams/{teamId}`
```typescript
interface TeamDocument {
  id: string;               // Unique Team ID (e.g., 'team_alpha')
  name: string;             // Display name (unique)
  tag: string;              // 2-5 letter squad tag (unique)
  captainUserId: string;    // Reference to User
  captainName: string;      // Captain IGN
  goldPoints: number;       // In-app Gold Points balance (>= 0)
  registrationDate: number; // Unix timestamp
  isSuspended: boolean;     // Admin moderation flag
  monthlyRewardLastClaimedMonth: string; // e.g., '2026-10'
  winsCount: number;
  matchesPlayed: number;
}
```

### `transactions/{txId}` (Immutable Audit Ledger)
```typescript
interface TransactionDocument {
  id: string;
  teamId: string;
  teamName: string;
  amount: number;           // Positive (credit) or negative (debit)
  balanceAfter: number;
  type: 'MONTHLY_REWARD' | 'SLOT_CREATION_FEE' | 'ADMIN_ADJUSTMENT' | 'ADMIN_REFUND' | 'SCRIM_PRIZE';
  reason: string;
  relatedSlotId?: string;
  idempotencyKey?: string;
  createdByUserId: string;
  timestamp: number;
}
```

### `customSlots/{slotId}`
```typescript
interface CustomSlotDocument {
  id: string;
  title: string;
  hostTeamId: string;
  hostTeamName: string;
  hostCaptainUserId: string;
  hostCaptainName: string;
  gameMode: string;
  mapName: string;
  matchDateTime: string;
  matchTimestamp: number;
  maxTeams: number;         // e.g., 12 squads
  filledTeamsCount: number;
  entryRequirements: string;
  rules: string;
  contactInstructions: string;
  status: 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'FULL' | 'CLOSED' | 'CANCELLED' | 'COMPLETED';
  creationFeePaid: number;  // 50
  createdAt: number;
}
```

### `customSlots/{slotId}/privateCredentials/{credId}`
```typescript
interface RoomCredentialsDocument {
  roomId: string;           // Free Fire Custom Room ID
  roomPassword: string;     // Free Fire Custom Room Password
  hostCaptainUserId: string;
  approvedParticipantUserIds: string[];
}
```

---

## 3. Cloud Functions Backend Blueprint (Node.js / TypeScript)

### Function 1: `publishCustomSlotWithFee`
```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

export const publishCustomSlotWithFee = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Login required.');
  
  const userId = context.auth.uid;
  const { slotData, roomId, roomPassword, idempotencyKey } = data;
  const db = admin.firestore();
  
  return await db.runTransaction(async (transaction) => {
    // 1. Check idempotency
    const txQuery = await transaction.get(db.collection('transactions').where('idempotencyKey', '==', idempotencyKey).limit(1));
    if (!txQuery.empty) {
      throw new functions.https.HttpsError('already-exists', 'Duplicate submission detected.');
    }
    
    // 2. Fetch User & Team
    const userDoc = await transaction.get(db.doc(`users/${userId}`));
    const user = userDoc.data();
    if (!user || !user.teamId) throw new functions.https.HttpsError('failed-precondition', 'Must belong to a team.');
    
    const teamRef = db.doc(`teams/${user.teamId}`);
    const teamDoc = await transaction.get(teamRef);
    const team = teamDoc.data();
    if (!team) throw new functions.https.HttpsError('not-found', 'Team not found.');
    if (team.isSuspended) throw new functions.https.HttpsError('permission-denied', 'Team is suspended.');
    
    // 3. Verify exactly 50 Gold Points balance
    const FEE = 50;
    if (team.goldPoints < FEE) {
      throw new functions.https.HttpsError('failed-precondition', `Insufficient balance. Hosting requires ${FEE} GP.`);
    }
    
    // 4. Atomic Deduct & Create
    const newBalance = team.goldPoints - FEE;
    const slotRef = db.collection('customSlots').doc();
    const txRef = db.collection('transactions').doc();
    
    transaction.update(teamRef, { goldPoints: newBalance });
    
    transaction.set(txRef, {
      id: txRef.id,
      teamId: team.id,
      teamName: team.name,
      amount: -FEE,
      balanceAfter: newBalance,
      type: 'SLOT_CREATION_FEE',
      reason: `Custom Room Slot Hosting Fee: ${slotData.title} (-50 GP)`,
      relatedSlotId: slotRef.id,
      idempotencyKey: idempotencyKey,
      createdByUserId: userId,
      timestamp: Date.now()
    });
    
    transaction.set(slotRef, {
      ...slotData,
      id: slotRef.id,
      hostTeamId: team.id,
      hostTeamName: team.name,
      hostCaptainUserId: userId,
      filledTeamsCount: 1,
      status: 'PUBLISHED',
      creationFeePaid: FEE,
      createdAt: Date.now()
    });
    
    // 5. Save protected credentials
    const credRef = slotRef.collection('privateCredentials').doc('secret');
    transaction.set(credRef, {
      roomId,
      roomPassword,
      hostCaptainUserId: userId,
      approvedParticipantUserIds: [userId]
    });
    
    return { success: true, slotId: slotRef.id, newBalance };
  });
});
```

### Function 2: `claimMonthlyTeamReward`
```typescript
export const claimMonthlyTeamReward = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Login required.');
  
  const userId = context.auth.uid;
  const { teamId, idempotencyKey } = data;
  const db = admin.firestore();
  
  const currentMonth = new Date().toISOString().slice(0, 7); // 'YYYY-MM'
  
  return await db.runTransaction(async (transaction) => {
    const teamRef = db.doc(`teams/${teamId}`);
    const teamDoc = await transaction.get(teamRef);
    const team = teamDoc.data();
    if (!team) throw new functions.https.HttpsError('not-found', 'Team not found.');
    if (team.isSuspended) throw new functions.https.HttpsError('permission-denied', 'Team is suspended.');
    
    if (team.monthlyRewardLastClaimedMonth === currentMonth) {
      throw new functions.https.HttpsError('failed-precondition', `Monthly reward already claimed for ${currentMonth}.`);
    }
    
    const REWARD = 1000;
    const newBalance = team.goldPoints + REWARD;
    const txRef = db.collection('transactions').doc();
    
    transaction.update(teamRef, {
      goldPoints: newBalance,
      monthlyRewardLastClaimedMonth: currentMonth
    });
    
    transaction.set(txRef, {
      id: txRef.id,
      teamId: team.id,
      teamName: team.name,
      amount: REWARD,
      balanceAfter: newBalance,
      type: 'MONTHLY_REWARD',
      reason: `${currentMonth} Monthly Team Promotional Reward (+1,000 GP)`,
      idempotencyKey,
      createdByUserId: userId,
      timestamp: Date.now()
    });
    
    return { success: true, newBalance };
  });
});
```

---

## 4. Testing & Running Instructions

1. **Local Emulator / JVM Unit Tests:**
   Run `gradle :app:testDebugUnitTest` to execute the comprehensive test suite covering:
   - Gold Points rankings descending order with tie-breakers.
   - Monthly reward eligibility (1,000 GP per month and duplicate prevention).
   - Atomic 50 GP slot creation fee debit.
   - Insufficient balance rejection.
   - Duplicate transaction idempotency protection.
   - Admin controlled refund workflow with audit trail.

2. **Android Phone Installation:**
   - Connect an Android device via USB with USB Debugging enabled (or open in the Streaming Android Emulator).
   - Run `gradle assembleDebug` to build the APK.
   - The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
