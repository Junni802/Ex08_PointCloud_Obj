package com.example.ex08_pointcloud_obj

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session



class MainActivity : AppCompatActivity() {
	var mSession: Session? = null
	var myGLRenderer:MyGLRenderer? = null
	var myGLView: GLSurfaceView? = null
	var objCnt: TextView? = null

	var tbList = mutableListOf<ToggleButton>()

	var objMap = mutableMapOf<String,MyObj>()

	inner class MyObj(val objName:String, val textureName:String, val scale:Float){

		fun makeObj(pose:Pose):ObjRenderer{
			val oo = ObjRenderer(  this@MainActivity,objName,textureName)

			val matrix = FloatArray(16)
			Matrix.setIdentityM(matrix,0)
			Matrix.translateM(matrix,0,pose.tx(),pose.ty(),pose.tz())
			Matrix.scaleM(matrix,0,scale,scale,scale)
			oo.setModelMatrix(matrix)
			return oo
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		requestPermission()

		myGLView = findViewById(R.id.myGLView)
		objCnt = findViewById(R.id.objCnt)

		tbList.add(findViewById(R.id.tb_andy))
		tbList.add(findViewById(R.id.tb_desk))
		tbList.add(findViewById(R.id.tb_box))
		tbList.add(findViewById(R.id.tb_chair))

		objMap.put("안디",MyObj("andy.obj","andy.png", 1f))
		objMap.put("책상",MyObj("table.obj","table.jpg",0.0005f))
		objMap.put("박스",MyObj("crate.obj","Crate_Base_Color.png", 0.001f))
		objMap.put("의자",MyObj("chair.obj","chair.jpg",0.001f))

		myGLView!!.setEGLContextClientVersion(3)
		//일시중지시 EGLContext 유지여부
		myGLView!!.preserveEGLContextOnPause=true


		myGLRenderer = MyGLRenderer(this)
		//어떻게 그릴 것인가
		myGLView!!.setRenderer(myGLRenderer)

		//화면 렌더링을 언제 할 것인가 = 렌더러 반복호출하여 장면을 다시 그린다.
		myGLView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

		//화면 변화 감지
		val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager

		displayManager.registerDisplayListener(
			object: DisplayManager.DisplayListener{
				override fun onDisplayAdded(displayId: Int) {

				}

				override fun onDisplayRemoved(displayId: Int) {

				}

				override fun onDisplayChanged(displayId: Int) {
					synchronized(this){
						//화면 변경 와
						myGLRenderer!!.viewportChange = true
					}
				}

			}, null
		)
	}

	fun requestPermission(){
		ActivityCompat.requestPermissions(
			this,
			arrayOf(Manifest.permission.CAMERA),
			1234
		)
	}

	override fun onResume() {
		super.onResume()

		if(ArCoreApk.getInstance().requestInstall(this,true) ==
			ArCoreApk.InstallStatus.INSTALLED  ){
			mSession = Session(this)

			mSession!!.resume()

			Log.d("mSession 여","${mSession}")
		}
		myGLView!!.onResume()
	}

	override fun onPause() {
		super.onPause()

		mSession!!.pause()
		myGLView!!.onPause()
	}

	fun preRender(){
		// Log.d("preRender 여","gogo")

		//화면이 변환되었다면
		if(myGLRenderer!!.viewportChange){

			//회전상태 확인
			val display = windowManager.defaultDisplay

			//세션의 화면 정보 갱신
			//myGLRenderer!!.updateSession(mSession!!, display.rotation)
			mSession!!.setDisplayGeometry(display.rotation, myGLRenderer!!.width, myGLRenderer!!.height)
			//화면 변환 해제
			myGLRenderer!!.viewportChange = false

		}

		//이미 실제 카메라를 세션에서 적용
		// 렌더러에서 사용하도록 지정 --> CameraRenderer로 사용하도록 ID 설정
		mSession!!.setCameraTextureName(myGLRenderer!!.textureID)

		var frame: Frame? = null

		try {
			frame = mSession!!.update()
		}catch (e:Exception){

		}

		if(frame!=null) {  //frame이 null 이 되는 경우가 있어서 null이 아닐때만 실행
			myGLRenderer!!.mCamera.transformDisplayGeometry(frame!!)
		}

		//특징점을 획득한다.
		val pointCloud = frame!!.acquirePointCloud()

		myGLRenderer!!.mPointCloudRenderer.update(pointCloud)

		pointCloud.release()

		var mViewMatrix = FloatArray(16)
		var mProjMatrix = FloatArray(16)

		//frame의 camera의 viewMatrix ==> mViewMatrix에 대입입
		frame!!.camera.getViewMatrix(mViewMatrix,0)

		//frame의 camera의 projectionMatrix ==> mProjMatrix 대입입    near     far
		frame!!.camera.getProjectionMatrix(mProjMatrix,0, 0.01f, 100f)

		myGLRenderer!!.updateViewMatrix(mViewMatrix)
		myGLRenderer!!.updateProjMatrix(mProjMatrix)

		if(mTouched){

			mTouched = false

			var results = frame.hitTest(tapX, tapY)

			if(results.isNotEmpty()) {
				val pose = results[0].hitPose

				Log.d("pose 여", "${pose.tx()}, ${pose.ty()}, ${pose.tz()}")

//                val matrix = FloatArray(16)
//                Matrix.setIdentityM(matrix,0)

//                Matrix.translateM(matrix,0,pose.tx(),pose.ty(),pose.tz())
//                Matrix.scaleM(matrix,0,objMap[nowTb]!!.scale,objMap[nowTb]!!.scale,objMap[nowTb]!!.scale)

				//val oo = ObjRenderer(this, objMap[nowTb]!![0],objMap[nowTb]!![1])
				//val oo = ObjRenderer(this, objMap[nowTb]!!.objName,objMap[nowTb]!!.textureName)
				//oo.setModelMatrix(matrix)

				//myGLRenderer!!.objs.add(oo)
				//myGLRenderer!!.obj.setModelMatrix(matrix)

				myGLRenderer!!.objs.add(objMap[nowTb]!!.makeObj(pose))

				runOnUiThread{
					objCnt!!.text = "Obj : ${myGLRenderer!!.objs.size} 개"
				}
			}


		}

	}

	var tapX = 0f
	var tapY = 0f
	var mTouched = false

	override fun onTouchEvent(event: MotionEvent): Boolean {

		if(event.action==MotionEvent.ACTION_DOWN) {
			tapX = event.x
			tapY = event.y
			Log.d("onTouchEvent 여", "${event.x}, ${event.y}")
		}
		mTouched = true
		return false
	}

	var nowTb = "안디"

	fun tbGo(v:View){
		val btn = v as ToggleButton

		for(tb in tbList){

			tb.isChecked = false
		}
		btn.isChecked = true

		nowTb = btn.text.toString()
		Log.d("tbGo 여", nowTb)
	}

	fun deleteGo(v:View){

		if(myGLRenderer!!.objs.isNotEmpty()) {
			myGLRenderer!!.objs.removeLast()

			objCnt!!.text = "Obj : ${myGLRenderer!!.objs.size} 개"
		}
//		Log.d("deleteGo 여", "")
	}

	fun jundelete(v: View){
		Log.d("이상 혀", "${myGLRenderer!!.objs.get(1)}")
	}
}