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

    public static List<DocumentFile> listFiles(Context context, Uri treeUri) {
        List<DocumentFile> files = new ArrayList<>();
        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
        if (root != null && root.isDirectory()) {
            for (DocumentFile file : root.listFiles()) {
                files.add(file);
            }
        }
        return files;
    }

    public static String readFile(Context context, Uri fileUri) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(fileUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public static boolean writeFile(Context context, Uri treeUri, String filename, String content) {
        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
        if (root == null) return false;

        DocumentFile file = root.findFile(filename);
        if (file == null) {
            file = root.createFile("text/plain", filename);
        }

        if (file != null) {
            try (OutputStream os = context.getContentResolver().openOutputStream(file.getUri())) {
                os.write(content.getBytes());
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error writing file", e);
            }
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
}
