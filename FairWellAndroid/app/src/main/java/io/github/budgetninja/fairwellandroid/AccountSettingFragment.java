package io.github.budgetninja.fairwellandroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.CropImageIntentBuilder;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import static android.app.Activity.RESULT_OK;
import static android.os.Environment.isExternalStorageRemovable;
import static io.github.budgetninja.fairwellandroid.Utility.getDPI;


public class AccountSettingFragment extends Fragment {

    private static int REQUEST_PICTURE =1;
    private static int REQUEST_CROP_PICTURE = 2;

    private int DPI;
    private int PIXEL_PHOTO;
    private ParseFile userPhotoFile;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    ImageView userPhotoView;
    private ParseUser user;
    private ContentActivity parent;
    private View rootView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        user = ParseUser.getCurrentUser();
        parent = (ContentActivity)getActivity();
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);
        inflater.inflate(R.menu.menu_setting, menu);

        // Get max available VM memory, exceeding this capacity will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        // Initialize disk cache on background thread
        File cacheDir = getDiskCacheDir(parent.getApplicationContext(), FairwellApplication.DISK_CACHE_SUBDIR);
        new InitDiskCacheTask().execute(cacheDir);
        DPI = getDPI(parent.getApplicationContext());
        PIXEL_PHOTO = 100 * (DPI / 160);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_account_setting, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Account Setting");
        ((TextView) rootView.findViewById(R.id.email)).setText((user.getEmail()));
        if(user.get("profileName")==null||((String)(user.get("profileName"))).isEmpty()) {
            String profileNameString = user.get("First_Name") + " " + user.get("Last_Name");
            ((TextView) rootView.findViewById(R.id.profile_name_view)).setText(profileNameString);
        }else{
            ((TextView) rootView.findViewById(R.id.profile_name_view)).setText((String)(user.get("profileName")));
        }
        userPhotoView = (ImageView) rootView.findViewById(R.id.user_photo);
        userPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptUploadPhotoDialog();
            }
        });
        if(user != null){
            System.out.println("AAA");
            userPhotoFile = user.getParseFile("photo");
            if(userPhotoFile!=null) {
                System.out.println("BBB");
                loadParseFiletoImageView(userPhotoFile, userPhotoView, userPhotoFile.getName().substring(0, 48));
            }
        }

        System.out.println("CCC");
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                parent.mMenuDrawer.closeMenu(false);
                parent.fragMgr.popBackStack();
                return true;
            case R.id.action_save:
                saveAccountSetting();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final File croppedImageFile = new File(getActivity().getFilesDir(), "temp.jpg");
        Uri croppedImageUri = Uri.fromFile(croppedImageFile);
        if (requestCode==REQUEST_PICTURE&&resultCode == RESULT_OK) {
            CropImageIntentBuilder cropImage = new CropImageIntentBuilder(PIXEL_PHOTO, PIXEL_PHOTO, croppedImageUri);
            cropImage.setOutlineColor(0xFF03A9F4);
            cropImage.setSourceImage(data.getData());
            //requestCode*11 == code for Crop Picture
            startActivityForResult(cropImage.getIntent(getContext()), REQUEST_CROP_PICTURE);
        }
        if (requestCode == REQUEST_CROP_PICTURE && resultCode == Activity.RESULT_OK) {
            showProgressBar();
            final Bitmap photoBitmap = BitmapFactory.decodeFile(croppedImageFile.getAbsolutePath());
            ParseFile newPhoto = new ParseFile("photo.JPEG", getBytesFromBitmap(photoBitmap,50));
            user.put("photo", newPhoto);
            user.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        loadBitmap(getBytesFromBitmap(photoBitmap,50),
                                userPhotoView, user.getParseFile("photo").getName().substring(0, 48), PIXEL_PHOTO, PIXEL_PHOTO, null);
                    } else {
                        Toast.makeText(parent.getApplicationContext(), "Failed to upload new profile picture, please try again.", Toast.LENGTH_SHORT).show();
                        Log.d("User", "Failed to upload profile picture");
                    }
                }
            });
        }
    }
    public static byte[] getBytesFromBitmap(Bitmap bitmap,int rate) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, rate, stream);
        return stream.toByteArray();
    }

    private void loadParseFiletoImageView(ParseFile pf, final ImageView iv, final String keyProvided){
        final Bitmap bitmapInDisk = getBitmapFromDiskCache(keyProvided);
        if (bitmapInDisk != null) {

            System.out.println("DDD");
            loadBitmap(null, iv, keyProvided, PIXEL_PHOTO, PIXEL_PHOTO, bitmapInDisk);
        } else if(pf != null){

            System.out.println("EEE");
            pf.getDataInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] bytes, ParseException e) {
                    if (e == null) {

                        System.out.println("FFF");
                        loadBitmap(bytes, iv, keyProvided, PIXEL_PHOTO, PIXEL_PHOTO, bitmapInDisk);
                    } else {
                        Log.d("GetData", e.getMessage());
                        Toast.makeText(parent.getApplicationContext(),"Failed to load image",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public Bitmap getBitmapFromURI(Uri u){
        try {
            return MediaStore.Images.Media.getBitmap(parent.getContentResolver(), u);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        System.out.println("outHeight and outWidth" + height + "," + width);
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
        System.out.println("inSampleSize=" + inSampleSize);
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromByteArray(byte[] res, int reqWidth, int reqHeight) {
        System.out.println("decodeSampleBitmapFromByteArray: reqWidth+reqHeight = " + reqWidth + "," + reqHeight);
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(res, 0, res.length, options);
        System.out.println("decodeSampleBitmapFromByteArray InjustdecodeBounds: outWidth+outHeight = " + options.outWidth + "," + options.outHeight);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap result = BitmapFactory.decodeByteArray(res, 0, res.length, options);
        System.out.println("decodeSampleBitmapFromByteArray: resultWidth+resultHeight = " + result.getWidth() + "," + result.getHeight());
        return result;
    }

    class BitmapWorkerTask extends AsyncTask<byte[], Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private byte[] data;
        private String keyProvided;
        private int reqWidth;
        private int reqHeight;

        public BitmapWorkerTask(ImageView imageView, String s, int w,int h) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
            keyProvided = s;
            reqWidth = w;
            reqHeight = h;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(byte[]... params) {
            data = params[0];
            // Check disk cache in background thread
            Bitmap bitmap = getBitmapFromDiskCache(keyProvided);
            if (bitmap == null) {
                bitmap = decodeSampledBitmapFromByteArray(data, reqWidth, reqHeight);
                addBitmapToMemoryCache(keyProvided, bitmap);
                addBitmapToCache(keyProvided, bitmap);
            }
            return bitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (BitmapWorkerTask.this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
                hideProgressBar();
            }
        }
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public static Bitmap getBitmapFromByte(byte[] bt){
        return BitmapFactory.decodeByteArray(bt, 0, bt.length);
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    public void loadBitmap(byte[] sourceByteArray, ImageView imageView, String keyProvided, int w, int h, Bitmap bitmapInDisk) {
        if(sourceByteArray == null){

            System.out.println("GGG");
            imageView.setImageBitmap(bitmapInDisk);
            hideProgressBar();
        }else if (cancelPotentialWork(sourceByteArray, imageView)) {

            System.out.println("HHH");
            Bitmap bitmap = getBitmapFromMemCache(keyProvided);
            if(bitmap != null){

                System.out.println("III");
                imageView.setImageBitmap(bitmap);
                hideProgressBar();
            } else {

                System.out.println("JJJ");
                bitmap = getBitmapFromDiskCache(keyProvided);
                if (bitmap != null) {

                    System.out.println("KKK");
                    imageView.setImageBitmap(bitmap);
                    hideProgressBar();
                } else if(isAdded()){
                    System.out.println("LLL");
                    Log.d("loadBitmap", "Fragment Attached");
                    final BitmapWorkerTask task = new BitmapWorkerTask(imageView, keyProvided,w,h);
                    final AsyncDrawable asyncDrawable =
                            new AsyncDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.profilepic), task);
                    imageView.setImageDrawable(asyncDrawable);
                    task.execute(sourceByteArray);
                    hideProgressBar();
                } else {

                    System.out.println("MMM");
                    Log.d("loadBitmap", "Fragment Not Attached");
                }
            }
        }
    }

    public static boolean cancelPotentialWork(byte[] data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if(data==null){
            return true;
        }
        if (bitmapWorkerTask != null) {
            final byte[] bitmapData = bitmapWorkerTask.data;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData.length == 0 || bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (FairwellApplication.mDiskCacheLock) {
                File cacheDir = params[0];
                try {
                    mDiskLruCache = DiskLruCache.open(cacheDir, FairwellApplication.APP_VERSION,FairwellApplication.DISK_CACHE_COUNT,FairwellApplication.DISK_CACHE_SIZE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                FairwellApplication.mDiskCacheStarting = false;    // Finished initialization
                FairwellApplication.mDiskCacheLock.notifyAll();     // Wake any waiting threads
            }
            return null;
        }
    }

    public void addBitmapToCache(String key, Bitmap bitmap) {
        // Add to memory cache as before
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }

        // Also add to disk cache
        synchronized (FairwellApplication.mDiskCacheLock) {
            try {
                if (mDiskLruCache != null && mDiskLruCache.get(key) == null) {
                    final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                    OutputStream out;
                    if (editor != null) {
                        out = editor.newOutputStream(FairwellApplication.DISK_CACHE_INDEX);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        editor.commit();
                        out.close();
                    }
                    //mDiskLruCache.put(key, bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap getBitmapFromDiskCache(String key) {

        System.out.println("NNN");
        Bitmap bitmap = null;
        synchronized (FairwellApplication.mDiskCacheLock) {

            System.out.println("OOO");
            // Wait while disk cache is started from background thread
            while (FairwellApplication.mDiskCacheStarting) {

                System.out.println("PPP");
                try {
                    FairwellApplication.mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("QQQ");
            if (mDiskLruCache != null) {
                System.out.println("RRR");
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    InputStream inputStream;
                    if (snapshot != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(FairwellApplication.TAG, "Disk cache hit");
                        }
                        inputStream = snapshot.getInputStream(FairwellApplication.DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream).getFD();

                            // Decode bitmap, but we don't want to sample so give
                            // MAX_VALUE as the target dimensions
                            bitmap = getBitmapFromByte(IOUtils.toByteArray(inputStream));
                            //bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(fd, Integer.MAX_VALUE, Integer.MAX_VALUE, this);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
    // but if not mounted, falls back on internal storage.
    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir

        final String cachePath =
                (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !isExternalStorageRemovable()) ? getExternalCacheDir(context).getPath() :
                        context.getCacheDir().getPath();
        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * Get the external app cache directory.
     *
     * @param context The context to use
     * @return The external cache dir
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static File getExternalCacheDir(Context context) {
        if (VersionChecker.hasFroyo()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    public void promptUploadPhotoDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Upload a New Picture as Profile Photo?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //startActivityForResult(Intent.createChooser(new Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT), "Select picture"), REQUEST_PICTURE);
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_PICTURE);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void saveAccountSetting(){
        if(user!=null&&parent!=null){
            showProgressBar();
            String profileNameString = ((EditText)rootView.findViewById(R.id.profile_name)).getText().toString();
            final String firstNameString = ((EditText)rootView.findViewById(R.id.first_name)).getText().toString();
            final String lastNameString = ((EditText)rootView.findViewById(R.id.last_name)).getText().toString();
            String phoneNumberString = ((EditText)rootView.findViewById(R.id.phone_number)).getText().toString();
            String addressLine1String = ((EditText)rootView.findViewById(R.id.address_line_1)).getText().toString();
            String addressLine2String = ((EditText)rootView.findViewById(R.id.address_line_2)).getText().toString();
            String selfDescriptionString = ((EditText)rootView.findViewById(R.id.self_description)).getText().toString();
            if(!profileNameString.isEmpty()) {
                user.put("profileName", profileNameString);
            }else{
                user.put("profileName", firstNameString+" "+lastNameString);
            }
            user.put("First_Name",firstNameString);
            user.put("Last_Name",lastNameString);
            user.put("phoneNumber",phoneNumberString);
            user.put("addressLine1",addressLine1String);
            user.put("addressLine2",addressLine2String);
            user.put("selfDescription",selfDescriptionString);
            user.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    Toast.makeText(getContext(),"Data Saved",Toast.LENGTH_SHORT).show();
                    if(user.get("profileName")==null||((String)(user.get("profileName"))).isEmpty()) {
                        String profileNameString = user.get("First_Name") + " " + user.get("Last_Name");
                        ((TextView) rootView.findViewById(R.id.profile_name_view)).setText(profileNameString);
                    }else{
                        ((TextView) rootView.findViewById(R.id.profile_name_view)).setText((String)(user.get("profileName")));
                    }
                    hideProgressBar();
                }
            });

        }
    }

    private void showProgressBar(){
        View progressView = getActivity().findViewById(R.id.loadingPanel);
        if(progressView != null){
            progressView.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar(){
        View progressView = getActivity().findViewById(R.id.loadingPanel);
        if(progressView != null){
            progressView.setVisibility(View.GONE);
        }
    }
}
