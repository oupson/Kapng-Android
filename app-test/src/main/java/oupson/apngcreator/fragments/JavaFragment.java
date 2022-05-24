package oupson.apngcreator.fragments;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import oupson.apng.decoder.ApngDecoder;
import oupson.apng.decoder.ApngLoader;
import oupson.apngcreator.BuildConfig;
import oupson.apngcreator.R;


public class JavaFragment extends Fragment {
    private static final String TAG = "JavaActivity";

    private ApngLoader apngLoader = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (BuildConfig.DEBUG)
            Log.v(TAG, "onCreateView()");

        View v = inflater.inflate(R.layout.fragment_java, container, false);

        String imageUrl = "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png";
        ImageView imageView = v.findViewById(R.id.javaImageView);

        Context context = this.getContext();

        this.apngLoader = new ApngLoader();

        if (imageView != null && context != null) {
            if (BuildConfig.DEBUG)
                Log.v(TAG, "Loading " + imageUrl);
            this.apngLoader.decodeApngAsyncInto(context, imageUrl, imageView, new ApngLoader.Callback() {
                @Override
                public void onSuccess(@NotNull Drawable drawable) {
                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "Success");
                }

                @Override
                public void onError(@NotNull Throwable error) {
                    Log.e(TAG, "Error : " + error.toString());
                }
            }, new ApngDecoder.Config().setIsDecodingCoverFrame(false));
        }

        return v;
    }


    @Override
    public void onStop() {
        super.onStop();

        apngLoader.cancelAll();
    }
}
