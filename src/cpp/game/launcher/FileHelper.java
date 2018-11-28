package cpp.game.launcher;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.zip.GZIPOutputStream;

public class FileHelper
{
    private static final int BUFFER_SIZE = 32 * 1024;

    /**
     * Determine if {@code file} is a directory and is not a symbolic link.
     *
     * @param file File to test.
     * @return True if {@code file} is a directory and is not a symbolic link.
     * @throws IOException If a symbolic link could not be determined. This is ultimately
     *                     caused by a call to {@link File#getCanonicalFile()}.
     */
    public static boolean isPlainDir(File file) throws IOException
    {
        return file.isDirectory() && !isSymbolicLink(file);
    }

    /**
     * Given a {@link File} object, test if it is likely to be a symbolic link.
     *
     * @param file File to test for symbolic link.
     * @return {@code true} if {@code file} is a symbolic link.
     * @throws NullPointerException If {@code file} is null.
     * @throws IOException          If a symbolic link could not be determined. This is ultimately
     *                              caused by a call to {@link File#getCanonicalFile()}.
     */
    public static boolean isSymbolicLink(File file) throws IOException
    {
        if (file == null)
        {
            throw new NullPointerException("File must not be null");
        }

        File canon;
        if (file.getParent() == null)
        {
            canon = file;
        }
        else
        {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }

        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }

    public static String getUrlAsString(String url)
    {
        try
        {
            URL urlObj = new URL(url);
            URLConnection con = urlObj.openConnection();

            con.setDoOutput(true); // we want the response
            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            StringBuilder response = new StringBuilder();
            String inputLine;

            String newLine = System.getProperty("line.separator");
            while ((inputLine = in.readLine()) != null)
            {
                response.append(inputLine + newLine);
            }

            in.close();

            return response.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void compressGzipFile(String file, String gzipFile)
    {
        try
        {
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos = new FileOutputStream(gzipFile);
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[32 * 1024];
            int len;
            while ((len = fis.read(buffer)) != -1)
            {
                gzipOS.write(buffer, 0, len);
            }

            // close resources
            gzipOS.finish();
            gzipOS.close();
            fos.close();
            fis.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static byte[] getDigest(InputStream in, String algorithm) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        try
        {
            DigestInputStream dis = new DigestInputStream(in, md);
            byte[] buffer = new byte[BUFFER_SIZE];
            while (dis.read(buffer) != -1)
            {
                //
            }

            dis.close();
        }
        finally
        {
            in.close();
        }
        return md.digest();
    }

    public static String getDigestString(InputStream in, String algorithm) throws Exception
    {
        byte[] digest = getDigest(in, algorithm);
        return String.format("%064x", new java.math.BigInteger(1, digest));
    }

    public static String getSha256Hash(File f) throws Exception
    {
        return getDigestString(new FileInputStream(f), "SHA-256");
    }

    public static void writeBinaryFile(String path, byte[] content) throws IOException
    {
        FileOutputStream fos;
        fos = new FileOutputStream(path);
        fos.write(content);
        fos.close();
    }
}
