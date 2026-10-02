sed -i '/<uses-permission android:name="android.permission.CAMERA" \/>/d' app/src/main/AndroidManifest.xml
sed -i '/<uses-feature android:name="android.hardware.camera" android:required="false" \/>/d' app/src/main/AndroidManifest.xml
