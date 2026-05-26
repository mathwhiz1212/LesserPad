package org.pulpdust.lesserpad;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DocumentHelper {
    private static final String TAG = "DocumentHelper";

    public static List<DocumentFile> listFiles(Context context, Uri parentUri) {
        List<DocumentFile> files = new ArrayList<>();
        if (parentUri == null) return files;
        try {
            Uri childrenUri;
            if (DocumentsContract.isDocumentUri(context, parentUri)) {
                childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, 
                        DocumentsContract.getDocumentId(parentUri));
            } else {
                childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, 
                        DocumentsContract.getTreeDocumentId(parentUri));
            }

            try (android.database.Cursor cursor = context.getContentResolver().query(childrenUri,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                 DocumentsContract.Document.COLUMN_MIME_TYPE},
                    null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String docId = cursor.getString(0);
                        String mime = cursor.getString(1);
                        if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                            Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, docId);
                            files.add(DocumentFile.fromSingleUri(context, fileUri));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing files for URI: " + parentUri, e);
            // Fallback to basic DocumentFile if query fails
            try {
                DocumentFile df = DocumentFile.fromTreeUri(context, parentUri);
                if (df != null && df.isDirectory()) {
                    DocumentFile[] rootFiles = df.listFiles();
                    if (rootFiles != null) {
                        for (DocumentFile file : rootFiles) {
                            if (file.isFile()) files.add(file);
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Fallback listFiles failed", ex);
            }
        }
        return files;
    }

    public static List<DocumentFile> listDirs(Context context, Uri treeUri) {
        List<DocumentFile> dirs = new ArrayList<>();
        if (treeUri == null) return dirs;
        try {
            DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
            if (root != null && root.isDirectory()) {
                DocumentFile[] rootFiles = root.listFiles();
                if (rootFiles != null) {
                    for (DocumentFile file : rootFiles) {
                        if (file.isDirectory()) {
                            dirs.add(file);
                        }
                    }
                }
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException listing dirs for URI: " + treeUri, se);
        } catch (Exception e) {
            Log.e(TAG, "Error listing dirs for URI: " + treeUri, e);
        }
        return dirs;
    }

    public static String readFile(Context context, Uri fileUri) throws IOException {
        Log.d(TAG, "readFile: Reading URI: " + fileUri);
        if (isDirectory(context, fileUri)) {
            Log.e(TAG, "readFile: Aborting read because URI is a directory: " + fileUri);
            throw new IOException("EISDIR (Is a directory)");
        }
        StringBuilder content = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(fileUri)) {
            if (is == null) {
                Log.e(TAG, "readFile: InputStream is null for " + fileUri);
                throw new IOException("InputStream is null");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "readFile: Failed to read " + fileUri, e);
            throw e;
        }
        return content.toString();
    }

    public static boolean writeFile(Context context, Uri uri, String filename, String content) {
        if (uri == null) return false;
        try {
            DocumentFile file = null;
            // Try to find if the file already exists in this folder
            List<DocumentFile> existing = listFiles(context, uri);
            for (DocumentFile df : existing) {
                if (filename.equals(df.getName())) {
                    file = df;
                    break;
                }
            }

            if (file == null) {
                // If not found, try to create it
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    Uri newFileUri = DocumentsContract.createDocument(context.getContentResolver(), uri, "text/plain", filename);
                    if (newFileUri != null) {
                        file = DocumentFile.fromSingleUri(context, newFileUri);
                    }
                } else {
                    DocumentFile dir = DocumentFile.fromTreeUri(context, uri);
                    if (dir != null && dir.isDirectory()) {
                        file = dir.createFile("text/plain", filename);
                    }
                }
            }

            if (file != null) {
                try (OutputStream os = context.getContentResolver().openOutputStream(file.getUri())) {
                    os.write(content.getBytes());
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Error writing file", e);
                }
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException writing to URI: " + uri, se);
        } catch (Exception e) {
            Log.e(TAG, "Error processing URI: " + uri, e);
        }
        return false;
    }
    
    public static boolean deleteFile(Context context, Uri fileUri) {
        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), fileUri);
        } catch (IOException e) {
            Log.e(TAG, "Error deleting file", e);
            return false;
        }
    }

    public static boolean isSaf(Context context) {
        android.content.SharedPreferences sprefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return sprefs.getString("saf_uri", null) != null;
    }

    public static boolean isDirectory(Context context, Uri uri) {
        if (uri == null) return false;
        try {
            // A Tree URI is a directory permission, but individual document URIs 
            // inside a tree can also be flagged as Tree URIs by some providers.
            // We need to check if it's a "Tree Document" (a file inside a tree)
            // or a "Tree Root" (the folder itself).
            if (android.os.Build.VERSION.SDK_INT >= 24 && DocumentsContract.isTreeUri(uri)) {
                // If it contains "document", it's likely a specific file within the tree
                if (uri.toString().contains("/document/")) {
                    // Fall through to MIME type check to be sure
                } else {
                    return true;
                }
            }

            // Check MIME type directly via ContentResolver - this is the most reliable
            String mimeType = context.getContentResolver().getType(uri);
            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                return true;
            }

            // Check DocumentFile as a last resort
            DocumentFile df = DocumentFile.fromSingleUri(context, uri);
            return df != null && df.isDirectory();
        } catch (Exception e) {
            Log.w(TAG, "Error checking if URI is directory: " + uri, e);
            return false;
        }
    }

    public static Uri moveFile(Context context, Uri fileUri, Uri sourceFolderUri, Uri targetFolderUri) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                // DocumentsContract.moveDocument requires document URIs, not tree URIs, for source and target
                Uri sourceDocUri = sourceFolderUri;
                if (!DocumentsContract.isDocumentUri(context, sourceDocUri)) {
                    sourceDocUri = DocumentsContract.buildDocumentUriUsingTree(sourceDocUri, DocumentsContract.getTreeDocumentId(sourceDocUri));
                }
                
                Uri targetDocUri = targetFolderUri;
                if (!DocumentsContract.isDocumentUri(context, targetDocUri)) {
                    targetDocUri = DocumentsContract.buildDocumentUriUsingTree(targetDocUri, DocumentsContract.getTreeDocumentId(targetDocUri));
                }
                
                Log.d(TAG, "Moving " + fileUri + " from " + sourceDocUri + " to " + targetDocUri);
                return DocumentsContract.moveDocument(context.getContentResolver(), fileUri, sourceDocUri, targetDocUri);
            } catch (Exception e) {
                Log.e(TAG, "Error moving document", e);
            }
        }
        return null;
    }

    public static DocumentFile getOrCreateFolder(Context context, Uri treeUri, String folderName) {
        if (treeUri == null) return null;
        try {
            DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
            if (root == null || !root.isDirectory()) return null;
            DocumentFile folder = root.findFile(folderName);
            if (folder == null) {
                folder = root.createDirectory(folderName);
            }
            return folder;
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException in getOrCreateFolder for URI: " + treeUri, se);
        } catch (Exception e) {
            Log.e(TAG, "Error in getOrCreateFolder for URI: " + treeUri, e);
        }
        return null;
    }
}
