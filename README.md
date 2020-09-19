# Kapng-Android 
### An android library to create or display apng

# Example of apng :

![apng-example](https://upload.wikimedia.org/wikipedia/commons/1/14/Animated_PNG_example_bouncing_beach_ball.png)

# How to use this library :

## To load an animated png to an imageView : 
```kotlin
val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/1/14/Animated_PNG_example_bouncing_beach_ball.png"

ApngDecoder.decodeApngAsyncInto(context, URL(url), imageView)
```

You can load a file, an uri, a resource int, an url, or an inputStream.

## With a callback :
```kotlin
ApngDecoder.decodeApngAsyncInto(this, uri, viewerImageView, callback = object : ApngDecoder.Callback {
  override fun onSuccess(drawable: Drawable) { println("Success !") }
  override fun onError(error: Exception) { println("Error : $error") }
})
```

## To create animated png :

```kotlin
val output : OutputStream = FileOutputStream("res.png") // An OutputStream (ex : a FileOutputStream)
val maxWidth : Int = 256 // The width of your image (ex : 256)
val maxHeight : Int = 256// The height of your image (ex : 256)
val nFrame : Int = 2 // Number of frame (ex : 2)

val inputFrame1 : InputStream = FileInputStream("frame1.png") // Input stream of your frame 1 (ex : a FileInputStream)
val inputFrame2 : InputStream = FileInputStream("frame2.png") // Input stream of your frame 2 (ex : a FileInputStream)

val encoder = ApngEncoder(output, maxWidth, maxHeight, nFrame)

encoder.writeFrame(inputFrame1)
inputFrame1.close()

encoder.writeFrame(inputFrame2, delay=500f) // With delay ! Default is 1000ms
inputFrame2.close()

encoder.writeEnd()
output.close()
```

# How to install :
## Via jitpack   [![](https://jitpack.io/v/oupson/Kapng-Android.svg)](https://jitpack.io/#oupson/Kapng-Android)
```gradle
repositories {
  maven { url "https://jitpack.io" }
 }
 
 dependencies {
  implementation 'com.github.oupson:Kapng-Android:1.0.10-beta3'
 }
 ```
 
 ## Or put the aar file in /libs/ and verify that you have :
 ```gradle
  dependencies {
    implementation fileTree(include: ['*.aar'], dir: 'libs')
  }
