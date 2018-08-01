package com.shkmishra.lyrically

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.widget.NestedScrollView
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.NotificationCompat
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.*
import java.net.URLEncoder
import java.util.*

class LyricsService : Service() {


    private lateinit var title: String
    private lateinit var lyrics: String
    private var offlineMusic = false
    private var lyricsFiles: Array<File>? = null
    private var cacheAll = false

    private var track = ""
    private var artist = ""
    private lateinit var artistU: String
    private lateinit var trackU: String
    private lateinit var titleTV: TextView
    private lateinit var lyricsTV: TextView
    private lateinit var scrollView: NestedScrollView
    private lateinit var refresh: ImageView
    private lateinit var progressBar: ProgressBar
    private var notifID = 26181317
    private var songArrayList = ArrayList<Song>()
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var triggerParams: WindowManager.LayoutParams
    private lateinit var lyricsPanelParams: WindowManager.LayoutParams
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var bottomLayout: View
    private lateinit var trigger: View
    private lateinit var container: LinearLayout
    private var vibrator: Vibrator? = null
    private lateinit var windowManager: WindowManager
    private var pendingIntent: PendingIntent? = null
    private var asyncJob: Job? = null


    @SuppressLint("NewApi")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (!Settings.canDrawOverlays(this) || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_SHORT).show()
            return Service.START_STICKY
        }
        val mNotifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mBuilder = NotificationCompat.Builder(this)

        if (vibrator != null) { // this means the service is already running; so we just handle the intent and update the lyrics sheet
            // make the notification persistent
            mBuilder.setContentTitle("Lyrically")
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setSmallIcon(R.mipmap.ic_launcher)
            mNotifyManager.notify(
                    26181317,
                    mBuilder.build())
            handleIntent(intent)
            return Service.START_STICKY
        }
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        displayMetrics = DisplayMetrics()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val width = sharedPreferences.getInt("triggerWidth", 10) * 2
        val height = sharedPreferences.getInt("triggerHeight", 10) * 2

        cacheAll = sharedPreferences.getBoolean("cacheAll", false)
        getSongsList() // get list of songs present on the device
        val file = File(Environment.getExternalStorageDirectory(), "Lyrically")
        if (!file.exists())
            file.mkdirs()

        // params for the invisible trigger
        triggerParams = WindowManager.LayoutParams(
                width, height,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)


        val panelHeight = sharedPreferences.getInt("panelHeight", 60) * displayMetrics.heightPixels / 100

        // params for the lyrics panel
        lyricsPanelParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                panelHeight,

                WindowManager.LayoutParams.TYPE_PHONE,

                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)


        lyricsPanelParams.gravity = Gravity.BOTTOM
        lyricsPanelParams.x = 0
        lyricsPanelParams.y = 0


        val triggerPosition = Integer.parseInt(sharedPreferences.getString("triggerPos", "1")) // 1 = left side of the screen, 2 = right side
        val offset = sharedPreferences.getInt("triggerOffset", 10).toDouble() / 100

        when (triggerPosition) {
            1 -> triggerParams.gravity = Gravity.TOP or Gravity.START
            2 -> triggerParams.gravity = Gravity.TOP or Gravity.END
        }
        triggerParams.x = 0
        triggerParams.y = (displayMetrics.heightPixels - displayMetrics.heightPixels * offset).toInt()


        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        trigger = View(this)


        bottomLayout = layoutInflater.inflate(R.layout.lyrics_sheet, null)
        bottomLayout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        container = LinearLayout(this)

        // views in the lyrics panel
        scrollView = bottomLayout.findViewById(R.id.lyricsScrollView) as NestedScrollView
        titleTV = bottomLayout.findViewById(R.id.title) as TextView
        lyricsTV = bottomLayout.findViewById(R.id.lyrics) as TextView
        progressBar = bottomLayout.findViewById(R.id.progressbar) as ProgressBar

        lyricsTV.setTextColor(Color.parseColor(sharedPreferences.getString("lyricsTextColor", "#FFFFFF")))
        titleTV.setTextColor(Color.parseColor(sharedPreferences.getString("songTitleColor", "#fd5622")))
        bottomLayout.findViewById(R.id.content).setBackgroundColor(Color.parseColor(sharedPreferences.getString("panelColor", "#383F47")))
        progressBar.indeterminateDrawable.setColorFilter(Color.parseColor(sharedPreferences.getString("songTitleColor", "#fd5622")), android.graphics.PorterDuff.Mode.SRC_IN)
        refresh = bottomLayout.findViewById(R.id.refresh) as ImageView
        val swipeRefreshLayout = bottomLayout.findViewById(R.id.swipeRefresh) as SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            lyricsTV.text = ""
            lyricsTV.visibility = View.INVISIBLE
            artistU = artist.replace(" ".toRegex(), "+")
            trackU = track.replace(" ".toRegex(), "+")
            if (asyncJob != null && asyncJob!!.isActive) asyncJob?.cancel()
            fetchLyricsAsync()
        }

        // swipe listener to dismiss the lyrics panel
        bottomLayout.setOnTouchListener(SwipeDismissTouchListener(bottomLayout, Any(), object : SwipeDismissTouchListener.DismissCallbacks {
            override fun canDismiss(token: Any): Boolean {
                return true
            }

            override fun onDismiss(view: View, token: Any) {
                container.removeView(bottomLayout)
                windowManager.removeView(container)
            }
        }))


        val swipeDirection = Integer.parseInt(sharedPreferences.getString("swipeDirection", "1")) // swipe direction for the invisible trigger
        trigger.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeUp() {
                super.onSwipeUp()
                if (swipeDirection == 1) {
                    vibrate()
                    windowManager.addView(container, lyricsPanelParams)
                    container.addView(bottomLayout)
                    val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
                    bottomLayout.startAnimation(animation)
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                if (swipeDirection == 4) {
                    vibrate()
                    windowManager.addView(container, lyricsPanelParams)
                    container.addView(bottomLayout)
                    val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
                    bottomLayout.startAnimation(animation)
                }
            }

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                if (swipeDirection == 3) {
                    vibrate()
                    windowManager.addView(container, lyricsPanelParams)
                    container.addView(bottomLayout)
                    val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
                    bottomLayout.startAnimation(animation)
                }
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                if (swipeDirection == 2) {
                    vibrate()
                    windowManager.addView(container, lyricsPanelParams)
                    container.addView(bottomLayout)
                    val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
                    bottomLayout.startAnimation(animation)
                }
            }
        }
        )

        // handler to show the lyrics panel when the notification is clicked
        val handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                try {
                    windowManager.addView(container, lyricsPanelParams)
                    container.addView(bottomLayout)
                    val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
                    bottomLayout.startAnimation(animation)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }

            }
        }

        // removes the invisible trigger if you click on it; otherwise the area taken by the trigger seems unresponsive to the user
        trigger.setOnClickListener {
            windowManager.removeView(trigger)
            handler.postDelayed({ windowManager.addView(trigger, triggerParams) }, 5000)
        }


        windowManager.addView(trigger, triggerParams)


        val showLyrics = Intent(this, ShowLyrics::class.java)
        showLyrics.putExtra("messenger", Messenger(handler))
        pendingIntent = PendingIntent.getService(this, 1, showLyrics, PendingIntent.FLAG_UPDATE_CURRENT)


        mBuilder.setContentTitle("Lyrically")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MIN)
                .setSmallIcon(R.mipmap.ic_launcher)
        mNotifyManager.notify(
                notifID,
                mBuilder.build())

        handleIntent(intent)

        if (sharedPreferences.getBoolean("checkForUpdates", true))
            checkForUpdates()

        return Service.START_STICKY
    }


    private fun checkForUpdates() {
        val lastCheck = sharedPreferences.getInt("updateWeek", 0)
        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)

        if (currentWeek != lastCheck) {
            startService(Intent(this, UpdateService::class.java))
            sharedPreferences.edit().putInt("updateWeek", currentWeek).apply()
        }
    }

    //####
    private fun handleIntent(intent: Intent) {
        if (asyncJob != null && asyncJob!!.isActive) asyncJob?.cancel()
        try {
            // check if the song was changed by comparing the current artist and title with those received from the broadcast
            if (!artist.equals(intent.getStringExtra("artist"), ignoreCase = true) || !track.equals(intent.getStringExtra("track"), ignoreCase = true)) {

                // clear the lyrics sheet
                lyrics = ""
                progressBar.visibility = View.VISIBLE
                titleTV.text = ""
                lyricsTV.text = ""
                lyricsTV.visibility = View.INVISIBLE
                refresh.visibility = View.GONE
                offlineMusic = false

                artist = intent.getStringExtra("artist")
                track = intent.getStringExtra("track")
                title = "$artist - $track"

                titleTV.text = title


                for (song in songArrayList) {
                    if (song.artist.equals(artist, ignoreCase = true) && song.track.equals(track, ignoreCase = true)) { // check if the song is present on the device
                        lyrics = getLyrics(song) // gets the lyrics from the text file
                        if (lyrics != "") {  // indicates we have offline lyrics available
                            lyricsTV.text = lyrics
                            lyricsTV.visibility = View.VISIBLE
                            scrollView.fullScroll(ScrollView.FOCUS_UP)
                            refresh.visibility = View.GONE
                            progressBar.visibility = View.GONE

                        } else { // offline lyrics not found, fetch them from the Internet
                            lyricsTV.text = ""
                            lyricsTV.visibility = View.INVISIBLE
                            artistU = artist.replace(" ".toRegex(), "+")
                            trackU = track.replace(" ".toRegex(), "+")
                            fetchLyricsAsync()
                            break
                        }
                        offlineMusic = true
                    }
                }
                if (!offlineMusic) { // indicates that the song is being streamed
                    if (cacheAll) {
                        lyrics = getLyricsStreaming()
                        if (lyrics != "") {
                            lyricsTV.text = lyrics
                            lyricsTV.visibility = View.VISIBLE
                            scrollView.fullScroll(ScrollView.FOCUS_UP)
                            refresh.visibility = View.GONE
                            progressBar.visibility = View.GONE
                        } else {
                            artistU = artist.replace(" ".toRegex(), "+")
                            trackU = track.replace(" ".toRegex(), "+")
                            fetchLyricsAsync()
                        }
                    } else {
                        artistU = artist.replace(" ".toRegex(), "+")
                        trackU = track.replace(" ".toRegex(), "+")
                        fetchLyricsAsync()
                    }
                }

            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    private fun vibrate() {
        val vibrate = sharedPreferences.getBoolean("triggerVibration", true)
        if (vibrate) vibrator!!.vibrate(125)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    // remove the lyrics panel and the trigger and unregister the receiver
    override fun onDestroy() {
        super.onDestroy()
        try {
            val animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
            bottomLayout.startAnimation(animation)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {

                }

                override fun onAnimationEnd(animation: Animation) {
                    container.removeView(bottomLayout)
                    windowManager.removeView(container)
                }

                override fun onAnimationRepeat(animation: Animation) {

                }
            })
            windowManager.removeView(trigger)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }


    private fun getSongsList() {

        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION)
        val cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection, null, null)
        while (cursor!!.moveToNext()) {
            val artist = cursor.getString(1)
            val title = cursor.getString(2)
            val songID = java.lang.Long.parseLong(cursor.getString(0))
            val duration = java.lang.Long.parseLong(cursor.getString(3))
            if (duration / 1000 > 40) {
                songArrayList.add(Song(title, artist, songID))
            }
        }
        cursor.close()


    }

    private fun getLyrics(song: Song): String {

        val path = Environment.getExternalStorageDirectory().toString() + File.separator + "Lyrically/"
        val directory = File(path)
        if (directory.exists())
            lyricsFiles = directory.listFiles() // files present in the Lyrically folder

        if (lyricsFiles == null)
            return ""
        for (file in lyricsFiles!!) {
            if (file.name == song.id.toString() + ".txt") { // the text files are named after the song IDs

                var fileText = ""
                try {
                    val bufferedReader = BufferedReader(FileReader(file))
                    fileText = bufferedReader.use(BufferedReader::readText)

                    bufferedReader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                return fileText
            }
        }
        return ""
    }

    private fun getLyricsStreaming(): String {

        val fileName = Integer.toString((artist + track).hashCode())

        val path = Environment.getExternalStorageDirectory().toString() + File.separator + "Lyrically/"
        val directory = File(path)
        if (directory.exists())
            lyricsFiles = directory.listFiles()    // files present in the Lyrically folder

        if (lyricsFiles == null)
            return ""
        for (file in lyricsFiles!!) {
            if (file.name == "$fileName.txt") {    // the text files are named after the song IDs

                var fileText = ""
                try {
                    val bufferedReader = BufferedReader(FileReader(file))
                    fileText = bufferedReader.use(BufferedReader::readText)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                return fileText
            }
        }
        return ""
    }

    private fun saveLyricsOffline(lyrics: String) {
        for (song in songArrayList) {
            if (song.artist.equals(artist, ignoreCase = true) && song.track.equals(track, ignoreCase = true)) {
                val path = File(Environment.getExternalStorageDirectory().toString() + File.separator + "Lyrically/")
                val lyricsFile = File(path, song.id.toString() + ".txt")
                try {
                    val fileWriter = FileWriter(lyricsFile)
                    fileWriter.write(lyrics)
                    fileWriter.flush()
                    fileWriter.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun saveLyricsStreaming(lyrics: String) {
        val fileName = Integer.toString((artist + track).hashCode())

        val path = File(Environment.getExternalStorageDirectory().toString() + File.separator + "Lyrically/")
        val lyricsFile = File(path, "$fileName.txt")
        if (lyricsFile.exists())
            return
        try {
            val fileWriter = FileWriter(lyricsFile)
            fileWriter.write(lyrics)
            fileWriter.flush()
            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    // fetches the lyrics from the Internet
    private fun fetchLyricsAsync() {
        /*
         Currently using 3 providers : azlyrics, genius and lyrics.wikia; in that order
         Procedure :
         - Google the artist + song name + provider name
         - Grab the first result and if it is from the provider we wanted, get the lyrics
          */
        asyncJob = launch(UI) {
            progressBar.visibility = View.VISIBLE
            val result = async(context = CommonPool, parent = asyncJob) {
                try {
                    title = "$artist - $track"

                    var url = "https://www.google.com/search?q=" + URLEncoder.encode("lyrics+azlyrics+$artistU+$trackU", "UTF-8") // Google URL
                    var document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get()
                    var results = document.select("h3.r > a").first()

                    var lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&")) // grabbing the first result
                    val element: Element
                    var temp: String
                    println(url)
                    println(lyricURL)


                    if (lyricURL.contains("azlyrics.com/lyrics")) { // checking if from the provider we wanted
                        document = Jsoup.connect(lyricURL).userAgent(USER_AGENT).get()
                        var page = document.toString()

                        page = page.substring(page.indexOf("that. -->") + 9)
                        page = page.substring(0, page.indexOf("</div>"))
                        temp = page
                    } else {

                        url = "https://www.google.com/search?q=" + URLEncoder.encode("genius+" + artistU + "+" + trackU + "lyrics", "UTF-8")
                        document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get()

                        results = document.select("h3.r > a").first()
                        lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&"))
                        println(url)
                        println(lyricURL)

                        if (lyricURL.contains("genius")) {

                            document = Jsoup.connect(lyricURL).userAgent(USER_AGENT).get()

                            val selector = document.select("div.h2")
                            for (e in selector) {
                                e.remove()
                            }

                            element = document.select("div[class=song_body-lyrics]").first()
                            temp = element.toString().substring(0, element.toString().indexOf("<!--/sse-->"))
                        } else {

                            url = "https://www.google.com/search?q=" + URLEncoder.encode("lyrics.wikia+$trackU+$artistU", "UTF-8")
                            document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get()

                            results = document.select("h3.r > a").first()
                            lyricURL = results.attr("href").substring(7, results.attr("href").indexOf("&"))
                            println(url)
                            println(lyricURL)

                            document = Jsoup.connect(lyricURL).userAgent(USER_AGENT).get()
                            element = document.select("div[class=lyricbox]").first()
                            temp = element.toString()

                        }
                    }

                    // preserving line breaks
                    temp = temp.replace("(?i)<br[^>]*>".toRegex(), "br2n")
                    temp = temp.replace("]".toRegex(), "]shk")
                    temp = temp.replace("\\[".toRegex(), "shk[")

                    lyrics = Jsoup.parse(temp).text()
                    lyrics = lyrics.replace("br2n".toRegex(), "\n")
                    lyrics = lyrics.replace("]shk".toRegex(), "]\n")
                    lyrics = lyrics.replace("shk\\[".toRegex(), "\n [")
                    if (lyricURL.contains("genius"))
                        lyrics = lyrics.substring(lyrics.indexOf("Lyrics") + 6)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@async null
                }
                return@async LyricsResponse(title, lyrics)
            }.await()
            progressBar.visibility = View.GONE
            if (result == null) { // if no lyrics found
                lyricsTV.text = ""
                lyricsTV.visibility = View.INVISIBLE
                progressBar.visibility = View.GONE
                titleTV.text = resources.getString(R.string.noLyricsFound)
                refresh.visibility = View.VISIBLE
                refresh.setOnClickListener {
                    if (asyncJob != null && asyncJob!!.isActive) asyncJob?.cancel()
                    fetchLyricsAsync()
                }
            } else {
                refresh.visibility = View.GONE
                progressBar.visibility = View.GONE
                scrollView.fullScroll(ScrollView.FOCUS_UP)
                lyricsTV.text = lyrics
                if (lyricsTV.visibility != View.VISIBLE)
                    lyricsTV.visibility = View.VISIBLE

                saveLyricsOffline(lyrics) // store the lyrics in a text file
                if (cacheAll) saveLyricsStreaming(lyrics)

            }
        }
    }

}
