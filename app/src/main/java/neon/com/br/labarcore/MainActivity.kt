package neon.com.br.labarcore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.ar.core.*
import com.google.ar.sceneform.ux.ArFragment

import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.rendering.Renderable
import kotlinx.android.synthetic.main.activity_main.*
import com.google.ar.core.AugmentedImageDatabase
import android.graphics.BitmapFactory
import android.widget.Toast
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"

        private const val KEY_ITAU = "itau"
        private const val KEY_DINHEIRO = "dinheiro"
        private const val KEY_NEON = "neon"
    }

    private val contentView by lazy {
        findViewById<View>(android.R.id.content)
    }

    private val session by lazy { Session(this) }

    private val model by lazy {
        Uri.parse("model.sfb")
    }

    private val images = mutableMapOf(
        KEY_DINHEIRO to R.raw.vinte_reais
//        KEY_DINHEIRO to R.raw.republica
//        KEY_DINHEIRO to R.raw.cinquenta_reais,
//        KEY_DINHEIRO to R.raw.dez_reais,
//        KEY_DINHEIRO to R.raw.cinco_reais
    )

    private val imageDatabase by lazy {
        AugmentedImageDatabase(session).apply {
            try {
                images.forEach {
                    val bitmap = resources.openRawResource(it.value)
                        .use { inputStream -> BitmapFactory.decodeStream(inputStream) }
                    addImage(it.key, bitmap)
                }
            } catch (e: IOException) {
                Log.e(TAG, "I/O exception loading augmented image bitmap.", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val config = Config(session).apply {
            augmentedImageDatabase = imageDatabase
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        }
        session.configure(config)
        arSceneView.setupSession(session)

        arSceneView.scene.addOnUpdateListener {
            arSceneView.arFrame?.getUpdatedTrackables(AugmentedImage::class.java)?.forEach {
                if (it.trackingState == TrackingState.TRACKING) {
                    when (it.name) {
                        KEY_ITAU -> openItauTaxes()
                        KEY_DINHEIRO -> openFocoNoDinheiro()
                        KEY_NEON -> openNeon()
                    }
                }
            }
        }

        fab.setOnClickListener {
            addObject()
        }
    }

    override fun onResume() {
        super.onResume()
        arSceneView.resume()
        session.resume()
    }

    override fun onPause() {
        super.onPause()
        arSceneView.pause()
        session.pause()
    }

    private fun addObject() {
        val frame = arSceneView.arFrame
        val (x, y) = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null) {
            hits = frame.hitTest(x, y)
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    placeObject(hit.createAnchor())
                    break
                }
            }
        }
    }

    private fun placeObject(anchor: Anchor) {
        ModelRenderable.builder()
            .setSource(this@MainActivity, model)
            .build()
            .thenAccept { renderable ->
                addNodeToScene(anchor, renderable as Renderable)
            }
            .exceptionally { throwable ->
                AlertDialog.Builder(this@MainActivity).apply {
                    setTitle("Codelab error!")
                    setMessage(throwable.message)
                    show()
                }
                null
            }
    }

    private fun addNodeToScene(anchor: Anchor, renderable: Renderable) {
        val selectionVisualizer = FootprintSelectionVisualizer()

        val transformationSystem =
            TransformationSystem(resources.displayMetrics, selectionVisualizer)

        val anchorNode = AnchorNode(anchor)
        arSceneView.scene.addChild(anchorNode)
        val node = TransformableNode(transformationSystem).apply {
            this.renderable = renderable
            setParent(anchorNode)
        }
        node.select()
    }

    private fun getScreenCenter(): Pair<Float, Float> {
        return Pair(contentView.width / 2f, contentView.height / 2f)
    }

    private fun openItauTaxes() {
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.itau.com.br/conta-corrente/tarifas/")
            startActivity(this)
        }
    }

    private fun openFocoNoDinheiro() {
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://focanodinheiro.neon.com.br/")
            startActivity(this)
        }
    }

    private fun openNeon() {
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://neon.com.br/")
            startActivity(this)
        }
    }
}
