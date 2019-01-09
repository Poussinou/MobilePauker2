/* 
 * Copyright 2011 Brian Ford
 * 
 * This file is part of Pocket Pauker.
 * 
 * Pocket Pauker is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or (at your option) any later version.
 * 
 * Pocket Pauker is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 * 
 * See http://www.gnu.org/licenses/.

*/

package com.daniel.mobilepauker2;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;

import java.io.File;
import java.io.FileFilter;

public class PaukerManager {

    private static PaukerManager instance = null;

    private String mCurrentFileName = Constants.DEFAULT_FILE_NAME;
    private String mFileAbsolutePath = null;
    private String mFileDropboxPath = null;
    private boolean mSaveRequired = false;

    // The application data directory can change when a file is loaded
    private String mApplicationDataDirectory = Constants.DEFAULT_APP_FILE_DIRECTORY;

    private PaukerManager() {}

    public static PaukerManager instance() {
        if (instance == null) {
            instance = new PaukerManager();
        }
        return instance;
    }

    /**
     * Checks if the App directory exists.
     * <p>
     * App directory name is set in GlobalPreferences.APP_FILE_DIRECTORY
     */
    public boolean checkAppDataDirectory() {
        File appDirectory = new File(Environment.getExternalStorageDirectory() + mApplicationDataDirectory);

        return appDirectory.exists() && appDirectory.isDirectory();
    }

    public void killLesson() {
        ModelManager.instance().clearLesson();
        mFileAbsolutePath = null;
        mFileDropboxPath = null;
        mCurrentFileName = null;

    }

    public boolean createDefaultAppDataDirectory() {
        File appDirectory = new File(Environment.getExternalStorageDirectory() + Constants.DEFAULT_APP_FILE_DIRECTORY);

        if (appDirectory.exists() && appDirectory.isDirectory()) {
            return true;
        } else {
            File parentDirectory = new File(Environment.getExternalStorageDirectory().getPath());

            if (parentDirectory.exists() && parentDirectory.canWrite()) {
                if (appDirectory.mkdir()) {
                    return true;
                } else {
                    Log.e("AndyPaukerApplication::createAppDirectory", "Unable to create directory");
                }
            }
        }

        return false;

    }

    /**
     * Get the default application data directory.
     * <p>
     * Note that this is not necessarily where the current file has been loaded from.
     * @return Default application data directory
     */
    public String getApplicationDataDirectory() {
        return mApplicationDataDirectory;
    }

    /**
     * Setup a new lesson in the default application directory
     * <p>
     * Note the lesson is not created until it is saved
     * Note this method appends .pau.gz to the application filename.
     */
    public void setupNewApplicationLesson() {
        /*String _filename = filename + ".pau.gz";
        if (setCurrentFileName(_filename)) {
            mApplicationDataDirectory = Constants.DEFAULT_APP_FILE_DIRECTORY;
            String filePath = Environment.getExternalStorageDirectory() + getApplicationDataDirectory() + _filename;
            File file = new File(filePath);
            setCurrentFileName(file.getName());
            setFileAbsolutePath(file.getAbsolutePath());
            ModelManager.instance().createNewLesson(filename);
            return true;
        } else {
            return false;
        }*/
        mCurrentFileName = Constants.DEFAULT_FILE_NAME;
        ModelManager.instance().createNewLesson();
    }


    public String getFileAbsolutePath() {
        return mFileAbsolutePath;
    }

    public String getFileDropboxPath() {
        return mFileDropboxPath;
    }

    public boolean openedWithDB() {
        return mFileDropboxPath != null;
    }

    // Todo replace this with the File Class
    public boolean setFileAbsolutePath(String fileAbsolutePath) {
        // Validate the filename
        if (fileAbsolutePath == null) {
            return false;
        }

        mFileAbsolutePath = fileAbsolutePath;
        return true;
    }

    /**
     * Setzt den Pfad der Datei in Dropboy incl. Dateinamen.
     * @param fileDropboxPath Pfad plus Name
     * @return True, wenn Pfad gesetzt werden konnte
     */
    public boolean setFileDropboxPath(String fileDropboxPath) {
        // Validate the filename
        if (fileDropboxPath == null) {
            return false;
        }

        mFileDropboxPath = fileDropboxPath;
        return true;
    }

    public boolean setCurrentFileName(String filename) {
        if (!filename.endsWith(".pau.gz"))
            filename += ".pau.gz";

        // Validate the filename
        if (!isNameValid(filename)) return false;

        mCurrentFileName = filename;
        return true;
    }

    public boolean isNameValid(String filename) {
        if (filename == null || filename.isEmpty() || filename.equals(Constants.PAUKER_FILE_ENDING)) {
            return false;
        }

        return filename.endsWith(".pau.gz");
    }

    public String getCurrentFileName() {
        return mCurrentFileName;
    }

    public boolean isSaveRequired() {
        return mSaveRequired;
    }

    public void setSaveRequired(boolean saveRequired) {
        mSaveRequired = saveRequired;
    }

    public String getReadableFileName() {
        String filename = mCurrentFileName;

        if (filename.endsWith(Constants.PAUKER_FILE_ENDING)) {
            return filename.substring(0, filename.length() - 7);
        } else if (filename.endsWith(".pau") || filename.endsWith(".xml")) {
            return filename.substring(0, filename.length() - 4);
        } else {
            return filename;
        }
    }

    public boolean validateFilename(Context context, String filename) {
        if (filename == null) {
            Log.d("Validate Filename", "File name is invalid");
            return false;
        }

        if (!filename.endsWith(Constants.PAUKER_FILE_ENDING)) {
            Log.d("Validate Filename", "File not ending with .pau.gz");
            return false;
        }

        return true;
    }

    public File[] listFiles(final Context context) throws SecurityException {
        File appDirectory = new File(Environment.getExternalStorageDirectory() + getApplicationDataDirectory());
        File[] files;

        if (!appDirectory.exists() && !appDirectory.mkdir()) {
            throw new SecurityException();
        }

        if (appDirectory.exists() && appDirectory.isDirectory()) {
            files = appDirectory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return validateFilename(context, file.getName());
                }
            });
        } else {
            Toast.makeText(context, R.string.error_importflashcardfile_directory, Toast.LENGTH_LONG).show();
            return null;
        }
        return files;
    }

    public boolean isFileExisting(Context context, String fileName) {
        File[] files = listFiles(context);

        for (File file : files) {
            if (file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }
}
