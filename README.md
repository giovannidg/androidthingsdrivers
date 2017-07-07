# drivers_android_things
Some driver for Android Things, import them with few lines of code:

#On root build.gradle

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
#On app-level build.gradle

	dependencies {
      compile 'com.github.giovannidg.androidthingsdrivers:sf0180:0.2.1'
      compile 'com.github.giovannidg.androidthingsdrivers:adc0832:0.2.1'
	}
