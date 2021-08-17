package cpp.game.launcher;

import cpp.game.launcher.crypto.PrivateKeyReader;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.CRC32;

public class PatchCreator
{
    private static final int fileNameColumnWidth = 128;
    private static final int fileSizeColumnWidth = 16;
    private static BufferedWriter outputBw;
    private static long totalSize = 0;

    private static String uploadVersionDirectory = Constants.uploadDirectory;
    private static PrivateKey privateKey;

    public static void main(String[] args)
    {
        try
        {
            // Delete upload directory
            FileUtils.deleteDirectory(new File(Constants.uploadDirectory));

            // Create upload directory and the version subdirectory
            uploadVersionDirectory += "\\" + Constants.patchGameVersion;
            File uploadFilesDirectoryFile = new File(uploadVersionDirectory);
            uploadFilesDirectoryFile.mkdirs();

            // Load RSA private key
            privateKey = PrivateKeyReader.get(Constants.baseDirectory + "\\private_key.der");

            // Sign news file
            try
            {
                // copy news file to upload directory first
                String newsFile = Constants.uploadDirectory + "\\news.txt";
                Files.copy(Paths.get(Constants.baseDirectory + "\\news.txt"), Paths.get(newsFile));
                byte[] newsSignature = signSHA256RSA(Files.readAllBytes(Paths.get(newsFile)), privateKey);
                String newsSigFile = Constants.uploadDirectory + "\\news.sig";
                FileHelper.writeBinaryFile(newsSigFile, newsSignature);

                System.out.println("Signed news file.");
            }
            catch (Exception e)
            {
                System.out.println("Skipping news file: " + e.getMessage());
            }

            // Get launcher checksum
            File launcher = new File(Constants.baseDirectory + "\\latest_launcher.exe");
            String launcherHash = FileHelper.getSha256Hash(launcher);

            // Copy latest launcher into the upload directory
            Files.copy(launcher.toPath(), Paths.get(Constants.uploadDirectory  + "\\latest_launcher.exe"));

            // Create latest.txt file
            FileWriter latest = new FileWriter(Constants.uploadDirectory + "\\latest.txt");
            String newLine = "\n";
            latest.write("game_version=" + Constants.patchGameVersion + newLine);
            latest.write("launcher_version=" + Constants.patchLauncherVersion + newLine);
            latest.write("launcher_checksum=" + launcherHash + newLine);
            latest.write("launcher_size=" + launcher.length() + newLine);
            latest.write("update_servers=" + Constants.numUpdateServers);
            latest.close();

            System.out.println("Created latest file.");

            // Sign latest file
            try
            {
                String file = Constants.uploadDirectory + "\\latest.txt";
                byte[] signature = signSHA256RSA(Files.readAllBytes(Paths.get(file)), privateKey);
                String newsSigFile = Constants.uploadDirectory + "\\latest.sig";
                FileHelper.writeBinaryFile(newsSigFile, signature);

                System.out.println("Signed latest file.");
            }
            catch (Exception e)
            {
                System.out.println("Skipping latest file");
            }

            // For writing files
            FileOutputStream fos;
            File fout;

            // Create checksums file
            fout = new File(Constants.uploadDirectory + "\\" + Constants.patchGameVersion + "\\checksums.txt");
            fos = new FileOutputStream(fout);
            outputBw = new BufferedWriter(new OutputStreamWriter(fos));

            // Create the patch
            System.out.println("Creating patch of: " + Constants.gameDirectory);
            iterateDirectory(Paths.get(Constants.gameDirectory));

            // Clean up
            outputBw.close();

            // Sign the checksums file, save the signature in checksums.sig
            byte[] checksumsSignature = signSHA256RSA(Files.readAllBytes(fout.toPath()), privateKey);
            fos = new FileOutputStream(Constants.uploadDirectory + "\\" + Constants.patchGameVersion + "\\checksums.sig");
            fos.write(checksumsSignature);
            fos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void iterateDirectory(Path startPath)
    {
        Queue<File> dirsq = new LinkedList<>();
        dirsq.add(startPath.toFile());
        int files = 0;
        int dirs = 0;
        try
        {
            dirs++; // to count the initial dir.
            while (!dirsq.isEmpty())
            {
                for (File f : dirsq.poll().listFiles())
                {
                    if (FileHelper.isPlainDir(f))
                    {
                        dirsq.add(f);
                        dirs++;
                    }
                    else if (f.isFile())
                    {
                        files++;
                        String fileName = f.getName();
                        boolean excludeFile = false;
                        for (String excludedFile : Constants.excludedFiles)
                        {
                            if (fileName.equals(excludedFile))
                            {
                                excludeFile = true;
                                break;
                            }
                        }

                        if (!excludeFile)
                        {
                            String path = f.getPath();
                            processFile(Constants.gameDirectory, path.substring(startPath.toString().length() + 1, path.length()));
                        }
                    }
                }
            }

            System.out.format("Files: %d, dirs: %d.\n", files, dirs);
            System.out.println("Created patch for game version " + Constants.patchGameVersion + ", launcher version " + Constants.patchLauncherVersion + ", total size: " + totalSize / (1024 * 1024) + " MB");
            System.out.println("Directory contents to upload: " + Constants.uploadDirectory);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static void processFile(String rootDirectory, String fileName) throws Exception
    {
        // Calculate CRC32
        String pathName = rootDirectory + "\\" + fileName;
        File file = new File(pathName);
        InputStream in = new FileInputStream(file);
        CRC32 crcMaker = new CRC32();
        byte[] buffer = new byte[32 * 1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1)
        {
            crcMaker.update(buffer, 0, bytesRead);
            totalSize += bytesRead;
        }
        long crcLong = crcMaker.getValue();
        String crcHex = String.format("%08X", crcLong);

        // Write output
        String output = fileName.replaceAll("\\\\", "/");

        // File name
        int amountOfTabs = (int) Math.ceil((fileNameColumnWidth - fileName.length()) / 4.0); // spacers
        for (int i = 0; i < amountOfTabs; i++)
            output += "\t";

        // File size
        String fileSize = Long.toString(file.length());
        output += fileSize;
        amountOfTabs = (int) Math.ceil((fileSizeColumnWidth - fileSize.length()) / 4.0); // spacers
        for (int i = 0; i < amountOfTabs; i++)
            output += "\t";

        // Make directories in the new folder
        Path sourcePath = Paths.get(pathName);
        Path targetPath = Paths.get(uploadVersionDirectory + "\\files\\" + fileName);
        targetPath.getParent().toFile().mkdirs();

        // Gzip the file
        String compressedFilePath = targetPath + ".gz";
        File compressedFile = new File(compressedFilePath);
        FileHelper.compressGzipFile(pathName, compressedFilePath);
        long compressedSize = compressedFile.length();

        // Write compressed size
        String compressedFileSize = Long.toString(compressedSize);
        output += compressedFileSize;
        amountOfTabs = (int) Math.ceil((fileSizeColumnWidth - compressedFileSize.length()) / 4.0); // spacers
        for (int i = 0; i < amountOfTabs; i++)
            output += "\t";

        // Write file checksum
        output += crcHex;

        // Calculate SHA256 hash for the file
        String sha256Hash = FileHelper.getSha256Hash(compressedFile);
        output += "\t" + sha256Hash;

        // Finalize the line in the checksum file
        outputBw.write(output + "\n");
    }

    // Create base64 encoded signature using SHA256/RSA.
    private static byte[] signSHA256RSA(byte[] input, PrivateKey privateKey) throws Exception
    {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(input);

        byte[] s = privateSignature.sign();
        return s;
    }
}
