package su.xash.cs16client;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/** Yuklangan yangi APK ni tizim o'rnatuvchisiga beradi (faqat update.apk, faqat o'qish uchun). */
public class ApkProvider extends ContentProvider {
    private File apk() {
        return new File(new File(getContext().getCacheDir(), "upd"), "update.apk");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = apk();
        if (!f.exists()) throw new FileNotFoundException("update.apk");
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        File f = apk();
        MatrixCursor c = new MatrixCursor(new String[]{"_display_name", "_size"});
        c.addRow(new Object[]{"CS16-boosttop.apk", f.length()});
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
