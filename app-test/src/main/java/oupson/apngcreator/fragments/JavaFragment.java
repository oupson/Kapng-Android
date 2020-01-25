package oupson.apngcreator.fragments;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import oupson.apng.ApngDecoder;
import oupson.apngcreator.R;


public class JavaFragment extends Fragment {
    private static final String TAG = "JavaActivity";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");

        View v = inflater.inflate(R.layout.fragment_java, container, false);

        String imageUrl = "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png";
        ImageView imageView = v.findViewById(R.id.javaImageView);

        Context context = this.getContext();

        if (imageView != null && context != null) {
            /*
            ApngAnimator a = ApngAnimatorKt.loadApng(imageView, imageUrl);
            a.onLoaded((animator) -> {
                animator.setOnFrameChangeLister((index) -> {
                    if (index == 0) {
                        Log.i(TAG, "Loop");
                    }
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
            */
            ApngDecoder.decodeApngAsyncInto(context, imageUrl, imageView, 1f, new ApngDecoder.Callback() {
                @Override
                public void onSuccess(@NotNull Drawable drawable) {
                    Log.i(TAG, "Success");
                }

                @Override
                public void onError(@NotNull Exception error) {
                    Log.e(TAG, "Error", error);
                }
            });

        }
        return v;
    }

}
