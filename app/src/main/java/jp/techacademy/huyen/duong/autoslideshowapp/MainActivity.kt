package jp.techacademy.huyen.duong.autoslideshowapp

import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import jp.techacademy.huyen.duong.autoslideshowapp.databinding.ActivityMainBinding
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val PERMISSIONS_REQUEST_CODE = 100
    private var index = 0

    // タイマー用の時間のための変数
    private var seconds = 0.0
    private var handler = Handler(Looper.getMainLooper())
    private var timer: Timer? = null
    private var checkStartOrPause = true // True: Start, False: Pause

    // APIレベルによって許可が必要なパーミッションを切り替える
    private val readImagesPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_IMAGES
        else android.Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        if (checkStartOrPause) {
            binding.start.setText("再生")
        } else {
            binding.start.setText("停止")
        }
        // パーミッションの許可状態を確認する
        if (checkSelfPermission(readImagesPermission) == PackageManager.PERMISSION_GRANTED) {
            // 許可されている
            onSlideShow()
        } else {
            // 許可されていないので許可ダイアログを表示する
            requestPermissions(
                arrayOf(readImagesPermission),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onSlideShow()
                }
        }
    }
    private fun onSlideShow() {
        val listUris = getContentsInfo()
        val uriSize = listUris.size
        if (uriSize > 0) {
            binding.imageview.setImageURI(listUris.get(0))
            binding.before.setOnClickListener() {
                onBefore(listUris, uriSize)
            }
            binding.next.setOnClickListener() {
                onNext(listUris, uriSize)
            }
            binding.start.setOnClickListener() {
                if (checkStartOrPause && timer == null) {
                    onStarts(listUris, uriSize)
                } else if (!checkStartOrPause && timer != null) {
                    onPauses()
                }
            }
        }
    }
    private fun getContentsInfo(): ArrayList<Uri> {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目（null = 全項目）
            null, // フィルタ条件（null = フィルタなし）
            null, // フィルタ用パラメータ
            null // ソート (nullソートなし）
        )
        var uris = ArrayList<Uri>()
        Log.d("uris", "" + uris.size)
        if (cursor!!.moveToFirst()) {
            // indexからIDを取得し、そのIDから画像のURIを取得する
            do {
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                Log.d("Uri", "" + imageUri)
                uris.add(imageUri)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return uris
    }

    private fun onPauses() {
        binding.start.setText("再生")
        binding.before.isEnabled = true
        binding.next.isEnabled = true
        timer!!.cancel()
        timer = null
        checkStartOrPause = true
    }

    private fun onStarts(listUris: kotlin.collections.ArrayList<Uri>, uriSize: Int) {
        binding.before.isEnabled = false
        binding.next.isEnabled = false
        binding.start.setText("停止")
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    binding.imageview.setImageURI(listUris.get(index))
                }
                if (index == uriSize) {
                    index = 0
                } else {
                    index++
                }
            }
        }, 200, 2000) // 最初に始動させるまで200ミリ秒、ループの間隔を2000ミリ秒 に設定
        checkStartOrPause = false
    }

    private fun onBefore(listUris: kotlin.collections.ArrayList<Uri>, uriSize: Int) {
        if (index == 0) {
            binding.imageview.setImageURI(listUris.get(uriSize - 1))
            index = uriSize - 1
        } else {
            binding.imageview.setImageURI(listUris.get(index - 1))
            index--
        }
    }

    private fun onNext(listUris: kotlin.collections.ArrayList<Uri>, uriSize: Int) {
        if (index == uriSize - 1) {
            binding.imageview.setImageURI(listUris.get(0))
            index = 0
        } else {
            binding.imageview.setImageURI(listUris.get(index + 1))
            index++
        }
    }
}