# Kapng-Android
An android library to create or display apng

Exemple of apng :

![apng-example](https://upload.wikimedia.org/wikipedia/commons/1/14/Animated_PNG_example_bouncing_beach_ball.png)

How to use this library :

To load animated png to an imageview : 
```kotlin

val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/1/14/Animated_PNG_example_bouncing_beach_ball.png" // image url could be an url, or a file path. You could also load byteArray and file

val animator = ApngAnimator(imageView)
animator.load(imageUrl)

animator.play()
```

To create animated png : WORK IN PROGRESS
