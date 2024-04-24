package com.simplecity.amp_library.utils;

import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.interfaces.FileType;
import com.simplecity.amp_library.model.BaseFileObject;
import com.simplecity.amp_library.model.FileObject;
import com.simplecity.amp_library.model.FolderObject;
import com.simplecity.amp_library.model.TagInfo;
import com.simplecity.amp_library.utils.sorting.SortManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileBrowser {
    @Nullable
    private File currentDir;

    private SettingsManager settingsManager;

    public FileBrowser(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    /**
     * Loads the specified folder.
     *
     * @param directory The file object to points to the directory to load.
     * @return An {@link List<BaseFileObject>} object that holds the data of the specified directory.
     */
    @WorkerThread
    public List<BaseFileObject> loadDir(File directory) {
        ThreadUtils.ensureNotOnMainThread();
        currentDir = directory;

        List<BaseFileObject> folderObjects = new ArrayList<>();
        List<BaseFileObject> fileObjects = new ArrayList<>();
        
        File[] files = directory.listFiles(FileHelper.getAudioFilter());
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDirectory(file, folderObjects);
                } else {
                    processFile(file, fileObjects);
                }
            }
        }
        
        finalizeLists(folderObjects, fileObjects);
        return folderObjects;
    }

    private void processDirectory(File file, List<BaseFileObject> folderObjects) {
        FolderObject folder = new FolderObject();
        folder.path = FileHelper.getPath(file);
        folder.name = file.getName();

        File[] listOfFiles = file.listFiles(FileHelper.getAudioFilter());
        if (listOfFiles != null && listOfFiles.length > 0) {
            countFilesAndFolders(listOfFiles, folder);
            if (!folderObjects.contains(folder)) {
                folderObjects.add(folder);
            }
        }
    }

    private void processFile(File file, List<BaseFileObject> fileObjects) {
        FileObject fileObj = new FileObject();
        fileObj.path = FileHelper.getPath(file);
        fileObj.name = FileHelper.getName(file.getName());
        fileObj.size = file.length();
        fileObj.extension = FileHelper.getExtension(file.getName());

        if (!TextUtils.isEmpty(fileObj.extension)) {
            fileObj.tagInfo = new TagInfo(fileObj.path);
            if (!fileObjects.contains(fileObj)) {
                fileObjects.add(fileObj);
            }
        }
    }

    private void countFilesAndFolders(File[] listOfFiles, FolderObject folder) {
        for (File file : listOfFiles) {
            if (file.isDirectory()) {
                folder.folderCount++;
            } else {
                folder.fileCount++;
            }
        }
    }

    private void finalizeLists(List<BaseFileObject> folderObjects, List<BaseFileObject> fileObjects) {
        sortFileObjects(fileObjects);
        sortFolderObjects(folderObjects);
        applySortSettings(folderObjects, fileObjects);
        folderObjects.addAll(fileObjects);
        addParentDirectory(folderObjects);
    }

    private void applySortSettings(List<BaseFileObject> folderObjects, List<BaseFileObject> fileObjects) {
        if (!settingsManager.getFolderBrowserFilesAscending()) {
            Collections.reverse(fileObjects);
        }
        if (!settingsManager.getFolderBrowserFoldersAscending()) {
            Collections.reverse(folderObjects);
        }
    }

    private void addParentDirectory(List<BaseFileObject> folderObjects) {
        if (!FileHelper.isRootDirectory(currentDir)) {
            FolderObject parent = new FolderObject();
            parent.fileType = FileType.PARENT;
            parent.name = FileHelper.PARENT_DIRECTORY;
            parent.path = FileHelper.getPath(currentDir) + File.separator + FileHelper.PARENT_DIRECTORY;
            folderObjects.add(0, parent);
        }
    }

    @Nullable
    public File getCurrentDir() {
        return currentDir;
    }

    @WorkerThread
    public File getInitialDir() {

        ThreadUtils.ensureNotOnMainThread();

        File dir;
        String[] files;

        String settingsDir = settingsManager.getFolderBrowserInitialDir();
        if (settingsDir != null) {
            File file = new File(settingsDir);
            if (file.exists()) {
                return file;
            }
        }

        dir = new File("/");

        files = dir.list((dir1, filename) -> dir1.isDirectory() && filename.toLowerCase().contains("storage"));

        if (files != null && files.length > 0) {
            dir = new File(dir + "/" + files[0]);
            //If there's an extsdcard path in our base dir, let's navigate to that. External SD cards are cool.
            files = dir.list((dir1, filename) -> dir1.isDirectory() && filename.toLowerCase().contains("extsdcard"));
            if (files != null && files.length > 0) {
                dir = new File(dir + "/" + files[0]);
            } else {
                //If we have external storage, use that as our initial dir
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    dir = Environment.getExternalStorageDirectory();
                }
            }
        } else {
            //If we have external storage, use that as our initial dir
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                dir = Environment.getExternalStorageDirectory();
            }
        }

        //Whether or not there was an sdcard, let's see if there's a 'music' dir for us to navigate to
        if (dir != null) {
            files = dir.list((dir1, filename) -> dir1.isDirectory() && filename.toLowerCase().contains("music"));
        }
        if (files != null && files.length > 0) {
            dir = new File(dir + "/" + files[0]);
        }

        return dir;
    }

    public void sortFolderObjects(List<BaseFileObject> baseFileObjects) {

        switch (settingsManager.getFolderBrowserFoldersSortOrder()) {
            case SortManager.SortFolders.COUNT:
                Collections.sort(baseFileObjects, fileCountComparator());
                Collections.sort(baseFileObjects, folderCountComparator());
                break;

            case SortManager.SortFolders.DEFAULT:
            default:
                Collections.sort(baseFileObjects, filenameComparator());
                break;
        }
    }

    public void sortFileObjects(List<BaseFileObject> baseFileObjects) {
        switch (settingsManager.getFolderBrowserFilesSortOrder()) {
            case SortManager.SortFiles.SIZE:
                Collections.sort(baseFileObjects, sizeComparator());
                break;
            case SortManager.SortFiles.FILE_NAME:
                Collections.sort(baseFileObjects, filenameComparator());
                break;
            case SortManager.SortFiles.ARTIST_NAME:
                Collections.sort(baseFileObjects, artistNameComparator());
                break;
            case SortManager.SortFiles.ALBUM_NAME:
                Collections.sort(baseFileObjects, albumNameComparator());
                break;
            case SortManager.SortFiles.TRACK_NAME:
                Collections.sort(baseFileObjects, trackNameComparator());
                break;
            case SortManager.SortFiles.DEFAULT:
            default:
                Collections.sort(baseFileObjects, trackNumberComparator());
                Collections.sort(baseFileObjects, albumNameComparator());
                Collections.sort(baseFileObjects, artistNameComparator());
                break;
        }
    }

    public void clearHomeDir() {
        settingsManager.setFolderBrowserInitialDir("");
    }

    public void setHomeDir() {
        if (currentDir != null) {
            settingsManager.setFolderBrowserInitialDir(currentDir.getPath());
        }
    }

    public File getHomeDir() {
        return new File(settingsManager.getFolderBrowserInitialDir());
    }

    public boolean hasHomeDir() {
        return !TextUtils.isEmpty(getHomeDir().getPath());
    }

    public boolean atHomeDirectory() {
        final File currDir = getCurrentDir();
        final File homeDir = getHomeDir();
        return currDir != null && homeDir != null && currDir.compareTo(homeDir) == 0;
    }

    public int getHomeDirIcon() {
        int icon = R.drawable.ic_folder_outline;
        if (atHomeDirectory()) {
            icon = R.drawable.ic_folder_remove;
        } else if (hasHomeDir()) {
            icon = R.drawable.ic_folder_nav;
        }
        return icon;
    }

    public int getHomeDirTitle() {
        int title = R.string.set_home_dir;
        if (atHomeDirectory()) {
            title = R.string.remove_home_dir;
        } else if (hasHomeDir()) {
            title = R.string.nav_home_dir;
        }
        return title;
    }

    private Comparator sizeComparator() {
        return (Comparator<BaseFileObject>) (lhs, rhs) -> (int) (rhs.size - lhs.size);
    }

    private Comparator filenameComparator() {
        return (Comparator<BaseFileObject>) (lhs, rhs) -> lhs.name.compareToIgnoreCase(rhs.name);
    }

    //    private Comparator durationComparator() {
    //        return (Comparator<FileObject>) (lhs, rhs) -> (int) (rhs.duration - lhs.duration);
    //    }

    private Comparator trackNumberComparator() {
        return (Comparator<FileObject>) (lhs, rhs) -> lhs.tagInfo.trackNumber - rhs.tagInfo.trackNumber;
    }

    private Comparator folderCountComparator() {
        return (Comparator<FolderObject>) (lhs, rhs) -> rhs.folderCount - lhs.folderCount;
    }

    private Comparator fileCountComparator() {
        return (Comparator<FolderObject>) (lhs, rhs) -> rhs.fileCount - lhs.fileCount;
    }

    private Comparator artistNameComparator() {
        return (Comparator<FileObject>) (lhs, rhs) -> {
            if (lhs.tagInfo.artistName == null || rhs.tagInfo.artistName == null) {
                return nullCompare(lhs.tagInfo.artistName, rhs.tagInfo.artistName);
            }
            return lhs.tagInfo.artistName.compareToIgnoreCase(rhs.tagInfo.artistName);
        };
    }

    private Comparator albumNameComparator() {
        return (Comparator<FileObject>) (lhs, rhs) -> {
            if (lhs.tagInfo.albumName == null || rhs.tagInfo.albumName == null) {
                return nullCompare(lhs.tagInfo.albumName, rhs.tagInfo.albumName);
            }
            return lhs.tagInfo.albumName.compareToIgnoreCase(rhs.tagInfo.albumName);
        };
    }

    private Comparator trackNameComparator() {
        return (Comparator<FileObject>) (lhs, rhs) -> {
            if (lhs.tagInfo.trackName == null || rhs.tagInfo.trackName == null) {
                return nullCompare(lhs.tagInfo.trackName, rhs.tagInfo.trackName);
            }
            return lhs.tagInfo.trackName.compareToIgnoreCase(rhs.tagInfo.trackName);
        };
    }

    <T extends Comparable<T>> int nullCompare(T a, T b) {
        return a == null ? (b == null ? 0 : Integer.MIN_VALUE) : (b == null ? Integer.MAX_VALUE : a.compareTo(b));
    }
}