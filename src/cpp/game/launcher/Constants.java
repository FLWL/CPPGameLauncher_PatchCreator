package cpp.game.launcher;

public class Constants
{
    public static final int patchGameVersion = 5; // version to package the game files as
    public static final int patchLauncherVersion = 102; // version to package the launcher as
    public static final int numUpdateServers = 1; // number of update servers

    public static final String baseDirectory = "C:\\Shared\\UnrealPackaged"; // working directory
    public static final String gameDirectory = baseDirectory + "\\WindowsNoEditor"; // the game files directory name in the working directory that the patch will be created of
    public static final String uploadDirectory = baseDirectory + "\\upload"; // the directory where the created patch files that need uploading go
    public static final String[] excludedFiles = {"Manifest_NonUFSFiles_Win32.txt", "Manifest_NonUFSFiles_Win64.txt",
            "Manifest_UFSFiles_Win32.txt", "Manifest_UFSFiles_Win64.txt"}; // files to exclude from the game files directory
}
