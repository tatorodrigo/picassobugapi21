package test.com.picassobugapi21;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadImage(new File(Environment.getExternalStorageDirectory(), "image_hangouts.jpg"));
    }

    private void loadImage(File file) {
        final ImageView image = (ImageView) findViewById(R.id.image_view);

        //não tocar no cache do picasso
        if (image.getTag() != null) {
            try {
                WeakReference<Bitmap> decodedBitmapOnErrorReference = (WeakReference<Bitmap>) image.getTag();
                Bitmap decodedBitmapOnError = decodedBitmapOnErrorReference.get();
                if (decodedBitmapOnError != null && !decodedBitmapOnError.isRecycled()) {
                    decodedBitmapOnError.recycle();
                }
                image.setTag(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Picasso picasso = new Picasso.Builder(this)
                .listener(new Picasso.Listener() {
                    @Override
                    public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                        //TODO checar melhor se a exceção é java.io.IOException: Cannot reset markableinputstream
                        if (exception.getMessage().contains("reset")) {
                            String path = getPath(uri);
                            if (path != null) {
                                Bitmap decodedBitmapOnError = decodeSampledBitmapFromPath(path, image.getWidth(), image.getHeight());
                                if (decodedBitmapOnError != null) {
                                    image.setImageBitmap(decodedBitmapOnError);
                                    image.setTag(new WeakReference<Bitmap>(decodedBitmapOnError));
                                }
                            }
                        }
                    }
                }).build();

        picasso.load(file).fit().into(image);
    }

    private Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public String getPath(Uri uri) {
        String path = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;

            try {
                cursor = getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    int column_index = cursor.getColumnIndexOrThrow("_data");
                    if (cursor.moveToFirst()) {
                        path = cursor.getString(column_index);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }

        return path;
    }
}
