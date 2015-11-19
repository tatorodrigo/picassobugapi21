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

        //em caso de erro do picasso, nós carregamos o bitmap no método onImageLoadFailed
        //e setamos uma WeakReference deste bitmap na tag do imageview
        //isto é importante pois nós não estamos preocupados em manter estes
        //bitmaps carregados em memória, então tão logo o imageview for reutilizado
        //nós reciclamos o bitmap antigo
        if (image.getTag() != null) {
            //bom, agora sabemos que a imageview tem uma tag que, esperançosamente,
            //fomos nós quem setamos no onImageLoadFailed
            //por isto, fazemos o cast para WeakReference<Bitmap> da tag do imageview
            try {
                WeakReference<Bitmap> decodedBitmapOnErrorReference = (WeakReference<Bitmap>) image.getTag();
                Bitmap decodedBitmapOnError = decodedBitmapOnErrorReference.get();
                if (decodedBitmapOnError != null && !decodedBitmapOnError.isRecycled()) {
                    //aqui é onde vamos liberar memória
                    //para não termos problema com o imageview (tentar usar um bitmap reciclado)
                    //nós setamos o imagebitmap para nulo antes de reciclar o bitmap
                    image.setImageBitmap(null);
                    decodedBitmapOnError.recycle();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                image.setTag(null);
            }
        }

        //aqui segue o fluxo normal, apenas instalamos um listener para sermos
        //notificado caso o picasso não consiga carregar a imagem
        Picasso picasso = new Picasso.Builder(this)
                .listener(new Picasso.Listener() {
                    @Override
                    public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                        //a exceção é java.io.IOException: Cannot reset markableinputstream
                        //issues relacionados:
                        // - https://github.com/square/picasso/issues/364
                        // - https://github.com/square/picasso/issues/465
                        // - https://github.com/square/picasso/issues/907
                        // - https://github.com/square/picasso/issues/983
                        // - etc

                        //sabemos que a exceção que lançada nos dispositivos contém a string
                        //"java.io.IOException: Cannot reset" na mensagem, então testamos
                        //apenas com "reset" para podermos tratar
                        if (exception.getMessage().contains("reset")) {
                            //fluxo normal, pegar o caminho físico para a imagem no dispositivo
                            //isso vai retornar algo do tipo /mnt/sdcard/myimage.jpg
                            String path = getPath(uri);
                            if (path != null) {
                                //decodificar a imagem com o tamanho menor
                                Bitmap decodedBitmapOnError = decodeSampledBitmapFromPath(path, image.getWidth(), image.getHeight());
                                if (decodedBitmapOnError != null) {
                                    //fluxo normal, setar o bitmap
                                    image.setImageBitmap(decodedBitmapOnError);

                                    //aqui entra uma parte importantíssima: setar a tag!
                                    //sem isso não seremos capaz de reciclar o bitmap no caso de reuso,
                                    //por exemplo, em um adapter do recyclerview
                                    image.setTag(new WeakReference<Bitmap>(decodedBitmapOnError));
                                }
                            }
                        }
                    }
                }).build();

        //fluxo normal, carregar usando o picasso!
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
