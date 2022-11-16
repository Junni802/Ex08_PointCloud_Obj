package com.example.ex08_pointcloud_obj


import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

//GLSurfaceView 를 렌더링하는 클래스
class MyGLRenderer(val mContext: MainActivity):GLSurfaceView.Renderer {

	var viewportChange = false

	//var obj:ObjRenderer

	var objs = ArrayList<ObjRenderer>()
	var mCamera:CameraRenderer
	var mPointCloudRenderer:PointCloudRenderer

	var width = 0
	var height = 0

	init{
		//obj = ObjRenderer(mContext,"andy.obj","andy.png")
//		objs.add(ObjRenderer(mContext,"andy.obj","andy.png"))

		mCamera = CameraRenderer()
		mPointCloudRenderer = PointCloudRenderer()
	}


	val textureID:Int
		get() = if(mCamera.mTextures==null) -1 else mCamera.mTextures!![0]

	override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
		GLES30.glEnable(GLES30.GL_DEPTH_TEST)  //3차원 입체감을 제공
		GLES30.glClearColor(1f,0.6f,0.6f,1f)
		Log.d("MyGLRenderer 여","onSurfaceCreated")


		//obj.init()
		mCamera.init()
		mPointCloudRenderer.init()
	}

	//화면크기가 변경시 화면 크기를 가져와 작업
	override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
		this.width = width
		this.height = height

		viewportChange = true

		Log.d("MyGLRenderer 여","onSurfaceChanged")
		GLES30.glViewport(0,0,width,height)

	}

	override fun onDrawFrame(gl: GL10?) {
		//Log.d("MyGLRenderer 여","onDrawFrame")
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

		mContext.preRender()  //그릴때 main의 preRender()를 실행한다.

		GLES30.glDepthMask(false)

		mCamera.draw()

		GLES30.glDepthMask(true)

		mPointCloudRenderer.draw()

		//obj.draw()
		for (oo in objs){
			if(oo.notInit){
				oo.init()
			}
			oo.draw()
		}
	}

	fun updateViewMatrix(matrix:FloatArray){
		mPointCloudRenderer.updateViewMatrix(matrix)
		for (oo in objs){
			oo.setViewMatrix(matrix)
		}
		//obj.setViewMatrix(matrix)
	}

	fun updateProjMatrix(matrix:FloatArray){
		mPointCloudRenderer.updateProjMatrix(matrix)
		for (oo in objs){
			oo.setProjectionMatrix(matrix)
//			objs.add(oo)
		}
		//obj.setProjectionMatrix(matrix)
	}
}