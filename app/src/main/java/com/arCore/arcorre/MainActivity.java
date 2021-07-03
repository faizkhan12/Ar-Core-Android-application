package com.arCore.arcorre;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.instantapps.InstantApps;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;

import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Collection;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private boolean isModelPlaced = false;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        // if you want 3d model to load every time we get a plane
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);

        // if you want 3d model to load only when user click on plane surface
        arFragment.setOnTapArPlaneListener(
                (HitResult hitresult, Plane plane, MotionEvent motionevent) -> {
                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING)
                        return;

                    Anchor anchor = hitresult.createAnchor();
                    placeModel(arFragment, anchor, Uri.parse("model.sfb"));
                }
        );
    }

    // fetch model during runtime
    private void placeModel(ArFragment arFragment, Anchor anchor, Uri uri) {
        ModelRenderable.builder()
                .setSource(arFragment.getContext(), uri)
                .build()
                .thenAccept(modelRenderable -> addNodeToScene(arFragment, anchor, modelRenderable))
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }

    private void addNodeToScene(ArFragment arFragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.getScaleController().setMinScale(2.0f);
        node.getScaleController().setMaxScale(3.0f);
        node.setLocalScale(new Vector3(2.8f, 2.8f, 2.8f));
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    /**
     * every time series updated , we will get a frame
     */
    private void onUpdate(FrameTime frameTime) {
        if (isModelPlaced)
            return;
        Frame frame = arFragment.getArSceneView().getArFrame();

        assert frame != null;
        Collection<Plane> planes = frame.getUpdatedTrackables(Plane.class);

        // go through each of the plane
        for (Plane plane : planes) {
            // check if plane is being tracked
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                // if yes - draw a anchor on top of it at the center
//                Anchor anchor = plane.createAnchor(plane.getCenterPose());
//                createCube(anchor);
                Anchor anchor = plane.createAnchor(plane.getCenterPose());
                placeModel(arFragment, anchor, Uri.parse("model.sfb"));
                break;
            }

        }
    }

    private void showInstallPrompt() {
        Intent postInstall = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage("com.example.arcorre");

        // The request code is passed to startActivityForResult().
        InstantApps.showInstallPrompt(MainActivity.this,
                postInstall, 100 , null);
    }

//    private void createCube(Anchor anchor) {
//        isModelPlaced = true;
//        MaterialFactory
//                .makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
//                .thenAccept(material -> {
//                    ModelRenderable cubeRenderable = ShapeFactory.makeSphere(0.3f,
//                            new Vector3(0f, 0.3f, 0f),material);
//
//                    AnchorNode anchorNode = new AnchorNode(anchor);
//                    anchorNode.setRenderable(cubeRenderable);
//                    arFragment.getArSceneView().getScene().addChild(anchorNode);
//                });
//    }
}