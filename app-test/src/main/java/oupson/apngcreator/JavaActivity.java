package oupson.apngcreator;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import kotlin.Unit;
import oupson.apng.ApngAnimator;
import oupson.apng.ApngAnimatorKt;
public class JavaActivity extends AppCompatActivity {
    static String TAG = "JavaActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java);
        String imageUrl = "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png";
        ImageView image = findViewById(R.id.javaImageView);
        ApngAnimator a =  ApngAnimatorKt.loadApng(image, imageUrl);
        a.onLoaded((animator) -> {
            animator.setOnFrameChangeLister((index) -> {
                if (index == 0) {
                    Log.i(TAG, "Loop");
                }
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

    }
}
