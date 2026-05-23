# Fixing Directory Persistence and File Placement in LesserPad

The goal of this task is to address two critical issues related to how LesserPad interacts with the Android Storage Access Framework (SAF):
1.  **Directory Persistence (Issue 1):** Ensuring that when the app restarts, the user's last selected subdirectory in the drop-down menu is restored correctly.
2.  **File Placement (Issue 2):** Ensuring that when the user selects a new directory while in 'File New' or 'File Open' mode, the application correctly sets the target folder URI for saving/writing, rather than attempting to physically move the currently opened file.

## User Review Required

- **File Open/Move Behavior:** When `fmode == FILE_OPEN`, selecting a different directory using the spinner (`doMove`) should change the target save location (`current_saf_uri`) for subsequent saves, not attempt to physically move the currently open file. I will implement this logic change. Please confirm this behavior aligns with user expectation.

## Proposed Changes

### LesserPadActivity.java

This file requires modifications to handle state restoration and directory navigation logic.

#### Changes in `listDirs()` (Persistence Fix)

The current implementation relies only on `current_saf_uri` to find the position. I will modify `listDirs()` to prioritize the saved `cur_pos` when available, and use `current_saf_uri` as a fallback/confirmation, thus ensuring the dropdown defaults to the user's last selection.

The updated logic in `listDirs()` will look for the index based on `cur_pos` (which is read from preferences) first.

#### Changes in `doMove()` (Placement Fix)

The current logic for `fmode == FILE_OPEN` in `doMove()` incorrectly calls `DocumentsContract.moveDocument`. This must be changed to simply update `current_saf_uri` to the URI of the selected subfolder (`targetFolderUri`) so that `textFiling` uses the correct path upon saving.

**`doMove` Diff Summary (Conceptual):**

```diff
-// Inside doMove, fmode == FILE_OPEN block:
-			try {
-				Uri newUri = DocumentsContract.moveDocument(context.getContentResolver(), getIntent().getData(), current_saf_uri, targetFolderUri);
-				if (newUri != null) {
-					current_saf_uri = targetFolderUri;
-					getIntent().setData(newUri);
-				}
-			} catch (Exception e) {
-				Log.e(TAG, "SAF Move failed", e);
-				Toast.makeText(context, R.string.mes_move_fail, Toast.LENGTH_SHORT).show();
-			}
+			// Inside doMove, fmode == FILE_OPEN block:
+			// Simply update current_saf_uri to the target folder URI for saving, without moving the file.
+			current_saf_uri = targetFolderUri;
```

---

## Verification Plan

### Automated Tests
- No specific unit tests exist in the provided code snippets that directly cover the SAF interaction flow (`listDirs` or `doMove`). Since these methods rely on the Android system (SAF) and file operations, true unit testing is complex.
- I will rely on a full application build and runtime verification.

### Manual Verification
- **Persistence Test (Issue 1):**
    1.  Run the application and navigate to a deep subdirectory (e.g., Folder A -> Folder B).
    2.  Close the application (kill the process or press back/home to exit the activity).
    3.  Relaunch the application.
    4.  Verify that the drop-down menu immediately shows the last selected subdirectory (Folder B).
- **Placement Test (Issue 2):**
    1.  Start a new file (`fmode == FILE_NEW`).
    2.  Select a target subdirectory (Folder A).
    3.  Write some text and save the file.
    4.  Close the app.
    5.  Relaunch the app and navigate to Folder B (a different subdirectory).
    6.  Verify that the saved file is correctly located in Folder A, confirming that the navigation did not move it.
    7.  Start a new file (`fmode == FILE_NEW`) and select Folder B. Write and save.
    8.  Verify the file is placed in Folder B.