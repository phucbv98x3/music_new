package com.example.monstarmusicnew.view.activity

import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.monstarmusicnew.R
import com.example.monstarmusicnew.adapter.SongAdapter
import com.example.monstarmusicnew.model.SongM
import com.example.monstarmusicnew.service.MusicService
import com.example.monstarmusicnew.view.fragment.OfflineFragment
import com.example.monstarmusicnew.viewmodel.MusicViewModel
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.content_activity.*
import kotlinx.android.synthetic.main.fragment_offline.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    View.OnClickListener {
    private var mOfflineFragment: OfflineFragment? = null
    var mMusicService: MusicService? = null
    private lateinit var mConnection: ServiceConnection
    private var mListPlay = mutableListOf<SongM>()
    private var isCheckBoundService: Boolean = false
    private var songM: SongM? = null
    private var mPosition: Int = 0
    private var mTimeCurrent = 0
    private lateinit var musicViewModel: MusicViewModel
    private lateinit var intentFil: IntentFilter
    var intentService = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        mOfflineFragment = OfflineFragment()
        musicViewModel = MusicViewModel()
        setSupportActionBar(toolbar)
        title = "Music OffLine"
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24)
        navigationView.setNavigationItemSelectedListener(this)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.content_manager_fragment, mOfflineFragment!!)
            .addToBackStack(null)
            .commit()
        startService()
        createConnection()
        clicksPlayMusic()
        intentFil = IntentFilter()
        intentFil.addAction(MusicService.ACTION_CLOSE)
        intentFil.addAction(MusicService.ACTION_NEXT)
        intentFil.addAction(MusicService.ACTION_PLAY)
        intentFil.addAction(MusicService.ACTION_PREVIOUS)
        registerReceiver(broadcastReceiver, intentFil)
       requestReadListMusicOffline()
    }
    private fun requestReadListMusicOffline() = if (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
           this,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )
    } else {
        getListOff()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getListOff()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun stopSV() {
        intentService.setClass(this, MusicService::class.java)
        stopService(intentService)
    }
    private fun getListOff() {
        musicViewModel?.getListMusicOffLine(contentResolver)
        musicViewModel?.listMusicOffline?.observe(this, androidx.lifecycle.Observer {
            ((this as AppCompatActivity)?.rcy_listOffline?.adapter as SongAdapter).setListMusic(it)
            this.mListPlay = it
        })
    }
    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val actionOnNotification = intent?.action
            if (mMusicService?.getMusicManager()?.mMediaPlayer!!.isPlaying) {
                btn_play.setImageResource(R.drawable.ic_baseline_pause_24)
            } else {
                btn_play.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }
            when (actionOnNotification) {
                MusicService.ACTION_CLOSE -> {
                    val notificationManager = getSystemService(
                        NOTIFICATION_SERVICE
                    ) as NotificationManager
                    unbindService(mConnection)
                    stopSV()
                    seekBar_time.progress = 0
                    tv_nameMusicShow.text = "My music xin chao"
                    tv_nameSingerShow.text = "Xin moi chon bai"
                    tv_time.text = "00:00"
                    tv_total_time.text = "00:00"
                    isCheckBoundService = false
                    btn_play.setImageResource(R.drawable.ic_baseline_play_arrow_24)
//                    if (isCheckBoundService) {
//                        unbindService(mConnection)
//                        //stopSV()
//                    }
                }
                MusicService.ACTION_NEXT -> {
                    if (mMusicService?.getMusicManager()?.mMediaPlayer?.isPlaying == true) {
                        if (mPosition < mListPlay.size - 1) {
                            mPosition += 1
                            btn_play.setImageResource(R.drawable.ic_baseline_pause_24)
                            tv_nameMusicShow.text = mListPlay[mPosition].songName
                            tv_nameSingerShow.text = mListPlay[mPosition].artistName
                            mMusicService?.playMusic(mListPlay[mPosition])
                            //mMusic=mListPlay[mPosition]
                        } else {
                            Toast.makeText(
                                this@HomeActivity,
                                "Không thể next bài",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(this@HomeActivity, "Không thể next bài", Toast.LENGTH_LONG)
                            .show()
                    }


                }
                MusicService.ACTION_PLAY -> {

                    mMusicService?.getMusicManager()?.mMediaPlayer?.let {
                        Log.d("Binh", "State: ${it.isPlaying}")

                        if (it.isPlaying) {
                            btn_play.setImageResource(R.drawable.ic_baseline_play_arrow_24)

                            songM?.let { itt ->
                                mMusicService?.pauseMusic(itt)

                            }
                        } else {
                            btn_play.setImageResource(R.drawable.ic_baseline_pause_24)

                            songM?.let { itt ->
                                mMusicService?.continuePlayMusic(itt)


                            }

                        }
                    }
                    Log.d("tesst",mMusicService?.getMusicManager()?.mMediaPlayer?.isPlaying.toString())
                }
                MusicService.ACTION_PREVIOUS -> {
                    mMusicService?.getMusicManager()?.mMediaPlayer?.let {
                        if (it.isPlaying) {
                            if (mListPlay.size > mPosition && mPosition >= 1) {
                                mPosition -= 1
                                btn_play.setImageResource(R.drawable.ic_baseline_pause_24)
                                tv_nameMusicShow.text = mListPlay[mPosition].songName
                                tv_nameSingerShow.text = mListPlay[mPosition].artistName
                                mMusicService?.playMusic(mListPlay[mPosition])
                            } else {
                                Toast.makeText(
                                    this@HomeActivity,
                                    "Không thể back bài",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@HomeActivity,
                                "Không thể back bài",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("show", "${mOfflineFragment?.isResumed}")
    }

    private fun clicksPlayMusic() {
        btn_play.setOnClickListener(this)
        btn_next.setOnClickListener(this)
        btn_previous.setOnClickListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when (p0.itemId) {
            R.id.list_music_of_nav -> {
                title = "Music OffLine"
                Log.d("ad", mOfflineFragment?.isAdded.toString())
                drawer_layout.closeDrawers()
                if (OfflineFragment().isResumed) {
                    supportFragmentManager
                        .beginTransaction()
                        .show(OfflineFragment())
                        .commit()

                } else {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.content_manager_fragment, OfflineFragment())
                        .commit()
                }


            }
            R.id.home_music_online_of_nav -> {
                title = "Music Online"
                drawer_layout.closeDrawers()
                if (Fragment().isAdded) {
                    supportFragmentManager
                        .beginTransaction()
                        .show(Fragment())
                        .commit()
                } else {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.content_manager_fragment, Fragment())
                        .addToBackStack(null)
                        .commit()
                }


            }

        }
        return true
    }

    private fun createConnection() {
        mConnection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
                isCheckBoundService = false
            }

            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                mMusicService = (service as MusicService.MyBinder).getService
                isCheckBoundService = true
                isCheckBoundService = true
                mMusicService?.musicFromService?.observe(
                    this@HomeActivity,
                    androidx.lifecycle.Observer {
                        songM = it
                        tv_nameMusicShow.text = it.songName
                        tv_nameSingerShow.text = it.artistName
                        val format = SimpleDateFormat("mm:ss", Locale.US)
                        mMusicService?.getMusicManager()?.durationMusic?.observe(
                            this@HomeActivity,
                            androidx.lifecycle.Observer { duration ->
                                val time = format.format(duration.toInt())
                                tv_total_time.text = time
                                mTimeCurrent = duration.toInt()
                                this@HomeActivity.seekBar_time.max = duration
                            })

                        if (mMusicService?.getMusicManager()?.mMediaPlayer?.isPlaying == true) {
                            btn_play.setImageResource(R.drawable.ic_baseline_pause_24)
                        }
                    })
                initSeekBar()
                runSeekBar()
            }
        }
        val intent = Intent()
        intent.setClass(this, MusicService::class.java)
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startService() {
        intentService.setClass(this, MusicService::class.java)
        startService(intentService)
    }

    private fun runSeekBar() {
        seekBar_time.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mMusicService?.getMusicManager()?.mMediaPlayer?.seekTo(seekBar!!.progress)
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })
    }

    private fun initSeekBar() {

        Log.d("ok", "${mTimeCurrent}")
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    mMusicService?.getMusicManager()?.mMediaPlayer?.let {
                        seekBar_time?.progress = it.currentPosition
                        val fm = SimpleDateFormat("mm:ss", Locale.US)
                        val time = fm.format(it.currentPosition)
                        tv_time.text = time
                        handler.postDelayed(this, 1000)
                    }

                } catch (ex: IOException) {
                    seekBar_time.progress = 0
                } catch (e: IllegalStateException) {
                    Log.d("ill", e.toString())
                }
            }

        }, 0)
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_play -> {
                if (mMusicService?.getMusicManager()?.mMediaPlayer == null) {
                    Toast.makeText(this, "Vui long chon bai hat !", Toast.LENGTH_LONG).show()
                } else {
                    if (!isCheckBoundService) {
                        createConnection()
                    } else {
                        mMusicService?.getMusicManager()?.mMediaPlayer?.let { it ->
                            if (it.isPlaying) {
                                btn_play.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                                mMusicService?.let {
                                    songM?.let { itt ->
                                        it.pauseMusic(itt)
                                    }
                                }
                            } else {
                                btn_play.setImageResource(R.drawable.ic_baseline_pause_24)
                                mMusicService?.let {
                                    songM?.let { itt ->
                                        it.continuePlayMusic(itt)
                                        //loopMusic()
                                    }
                                }
                            }
                        }
                    }

                }
            }

            R.id.btn_next -> {
                mMusicService?.getMusicManager()?.mMediaPlayer?.let {
                    if (it.isPlaying) {
                        btn_play.setImageResource(R.drawable.ic_baseline_pause_24)
                        if (mPosition < mListPlay.size - 1) {
                            mPosition += 1
                            tv_nameMusicShow.text = mListPlay[mPosition].songName
                            tv_nameSingerShow.text = mListPlay[mPosition].artistName
                            mMusicService?.playMusic(mListPlay[mPosition])
                            // loopMusic()
                        } else {
                            Toast.makeText(
                                this@HomeActivity,
                                "Không thể next bài",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@HomeActivity,
                            "Không thể next bài",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            R.id.btn_previous -> {
                mMusicService?.getMusicManager()?.mMediaPlayer?.let {
                    if (it.isPlaying) {
                        if (mListPlay.size > mPosition && mPosition >= 1) {
                            mPosition -= 1
                            btn_play.setImageResource(R.drawable.ic_baseline_pause_24)
                            tv_nameMusicShow.text = mListPlay[mPosition].songName
                            tv_nameSingerShow.text = mListPlay[mPosition].artistName
                            mMusicService?.playMusic(mListPlay[mPosition])
                            //loopMusic()
                        } else {
                            Toast.makeText(this, "Không thể back bài", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "Không thể back bài", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

}