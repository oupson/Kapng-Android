# Kapng-Android
An android library to create or display apng

Exemple of apng :

![apng-example](https://upload.wikimedia.org/wikipedia/commons/1/14/Animated_PNG_example_bouncing_beach_ball.png)

How to use this library :

To load animated png to an imageview : 
```kotlin

val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/1/14/Animated_PNG_example_bouncing_beach_ball.png" // image url could be an url, or a file path. You could also load byteArray and file

val animator = ApngAnimator(this).loadInto(imageView)
animator.load(imageUrl)

animator.play()
```

To create animated png :

```kotlin

val apng = Apng()

val file1 = File("image path 1")
val file2 = File("image path 2")

apng.addFrames(BitmapFactory.decodeByteArray(file1.readBytes(), 0, file1.readBytes().size))
apng.addFrames(BitmapFactory.decodeByteArray(file2.readBytes(), 0, file2.readBytes().size))

val apngByteArray = apng.generateAPNGByteArray()
File("output file path").writeBytes(apngByteArray)
```

How to install :
Via jitpack
```gradle
repositories {
  maven { url "https://jitpack.io" }
 }
 
 dependencies {
  implementation 'com.github.oupson:Kapng-Android:1.0.2'
 }
 ```
 
 Or put the aar file in /libs/ and verify that you have :
 ```gradle
  dependencies {
    implementation fileTree(include: ['*.aar'], dir: 'libs')
  }
