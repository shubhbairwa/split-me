# Project Plan

A bill splitting app called splitMe using MVVM, Material Design 3, and Jetpack Compose. Includes 'Me' profile auto-addition and 'Settle Up' features.

## Project Brief

# Project Brief: splitMe

splitMe is a modern, efficient Android application designed to simplify splitting bills and tracking group expenses. It leverages Material Design 3 to provide a vibrant, energetic, and user-friendly interface for managing personal and group debts.

## Features

- **Automatic User Membership**: Every group or expense created automatically includes the "Current User" (Self), ensuring the user's share is always accounted for without manual entry.
- **Group Settle Up**: A dedicated view to visualize net balances within a group. Users can perform a "settle up" action to record payments and clear outstanding debts between members.
- **Flexible Expense Splitting**: Create expenses with total amounts and split them among participants using various logic (equal split, specific amounts, or percentages).
- **"Me" Profile & Dashboard**: A personal profile representing the user that aggregates total balances (Owed vs. Owing) across all groups and individual transactions.

## High-Level Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with **Material Design 3** (M3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: **Jetpack Navigation 3** (state-driven navigation)
- **Adaptive Strategy**: **Compose Material Adaptive** library for responsive, multi-pane layouts.
- **Concurrency**: Kotlin Coroutines and Flow for reactive state management.

## Implementation Steps

### Task_1_DataLayer: Define Room entities (Member, Group, Bill, ExpenseShare), DAOs, and Repositories for local data persistence and state management.
- **Status:** COMPLETED
- **Updates:** Room entities (Member, Group, Bill, ExpenseShare, GroupMemberCrossRef), DAOs, and Repositories have been implemented. Relational POJOs for complex queries were also added. Database configuration is complete. SDK version adjusted to 35 for compatibility. Project builds successfully.
- **Acceptance Criteria:**
  - Room database and entities are correctly defined
  - DAOs and Repositories for CRUD operations are implemented
  - Project builds successfully

### Task_2_ManagementFeatures: Implement the UI and ViewModels for Member and Group management, allowing users to add and organize friends.
- **Status:** COMPLETED
- **Updates:** Implemented Member and Group management UI screens and ViewModels. Users can add/delete members and create groups. Integrated with the data layer. Material 3 theme with vibrant colors and edge-to-edge display implemented. Adaptive app icon created. Project builds successfully.
- **Acceptance Criteria:**
  - Screens for adding/editing members and groups are functional
  - ViewModels correctly handle data flow to the repository
  - UI follows Material 3 guidelines

### Task_3_BillSplittingFeature: Implement the bill entry screen with flexible split logic (equal/manual), ensuring the 'Me' profile is automatically included in every transaction and group.
- **Status:** COMPLETED
- **Updates:** Implemented bill entry screen with equal and manual split logic. Integrated 'Me' profile: ensured it is initialized on app start and automatically added to all new groups and expense splits. Updated GroupViewModel to auto-enroll 'Me'. Implemented Group Details screen with bill history. Added real-time validation for manual splits. Project builds successfully.
- **Acceptance Criteria:**
  - Bill entry works with total amount and category selection
  - 'Me' profile is automatically added to groups and expense splits
  - Equal and manual split calculations are accurate
  - Bills and shares are correctly saved to the database

### Task_6_SettleUpFeature: Develop the Group Settle Up functionality to visualize net balances among members and record payments to clear outstanding debts.
- **Status:** COMPLETED
- **Updates:** Implemented Settle Up functionality. Added logic to calculate net balances for group members. Created Settle Up screen to visualize debts and record payments. Integrated Settle Up action into Group Detail screen. Settlements are saved as special transactions in the database. Project builds successfully.
- **Acceptance Criteria:**
  - Settle Up view displays accurate net balances for all group members
  - Recording a settlement correctly updates the balance state in the database

### Task_4_DashboardNavigation: Create the main dashboard featuring the 'Me' profile aggregate balances (Owed vs. Owing) and implement responsive multi-pane Navigation 3.
- **Status:** COMPLETED
- **Updates:** Implemented Dashboard with aggregate balances for the 'Me' profile. Created logic to calculate 'You are owed' vs 'You owe' across all groups. Implemented adaptive navigation using NavigationSuiteScaffold for responsive layouts (switching between NavigationBar and NavigationRail). Dashboard is now the entry point. Project builds successfully.
- **Acceptance Criteria:**
  - Dashboard displays an accurate 'who owes what' summary including the user's total balance
  - Navigation 3 is implemented for screen transitions
  - Adaptive layouts correctly handle different screen sizes (multi-pane)

### Task_5_PolishVerification: Perform final verification and run stability checks to ensure alignment with user requirements and Material Design 3 guidelines.
- **Status:** COMPLETED
- **Updates:** Final verification completed by critic_agent.
- Stability: ANR when adding member fixed by offloading to background threads.
- Settle Up: Stale data issue fixed by keying ViewModel with groupId.
- UI/UX: Edge-to-Edge display corrected in Theme.kt.
- Logic: 'Me' profile is consistently added to all groups.
- Adaptive Layout: Navigation rail/bar switching verified.
- Core functions: Dashboard, Bill Splitting (Equal/Manual), Group Management, and Settle Up are all functional.
The app is stable and meets all requirements.
- **Acceptance Criteria:**
  - App does not crash and passes all build checks
  - 'Me' profile and Settle Up features work as expected
  - Vibrant Material 3 theme and Edge-to-Edge display are consistent
  - Final stability and requirement alignment verified by critic_agent
- **Duration:** N/A

