# Workaround for Multipart Form-data request in React

## Problem overview
See https://github.com/facebook/react-native/issues/44657

Trying to make a Multipart Form-data POST request from react-native/expo application leads to JavaScript non-descriptive
"Network error!". No HTTP request is attempted at all. It doesn't matter what JS HTTP Client you're using - fetch, axios
are getting the same result.

The problem happens only on Android devices, or so the others say, I haven't tested on iOS.

## Cause
The cause of the problem is a bug in [com.facebook.react.modules.network.NetworkingModule](https://github.com/facebook/react-native/blob/0.76-stable/packages/react-native/ReactAndroid/src/main/java/com/facebook/react/modules/network/NetworkingModule.java).
In method `constructMultipartBody(...)` the Content-type of the form data part is evaluated incorrectly, which makes 
the entire HTTP request to go silently into the trash and causing the error.

## Workaround
`NetworkingModule` allows certain customizations of the HTTP request by adding `RequestBodyHandler`, `ResponseHandler`,
`UriHandler`, or even by modifying the builder of the `OkHttpClient`. In our case we need to implement custom `RequestBodyHandler`
which will process Form Data payload instead of the standard React code.

The problem with this approach is that it is pretty hard to get hold on the `NetworkingModule` in React Native Android
application. Here are the steps that worked for me:

### 1. Eject the React Native app
For Expo application like the current one run
```console
$ npx expo prebuild
```
This will create android/ directory with the Android Gradle project.

### 2. Create `FormDataRequestBodyHandler`
For the fix I used the _debug_ variant of the application and the classes related to the fix are into 
[com.simeonkirov.expodebug.multipart](android/app/src/debug/java/com/simeonkirov/expodebug/multipart) package. 
[FormDataRequestBodyHandler](android/app/src/debug/java/com/simeonkirov/expodebug/multipart/FormDataRequestBodyHandler.java)
class implements the actual fix into `getPartContentType(...)` method.

### 3. Create React Module and Package
We `NetworkingModule` which is created and registered in the MainPackage by the React application. You can get already
instantiated `NativeModule`s  from `ReactContext`, but it appeared that getting `ReactContext` is not trivial. Generally,
it should be something like this:
```java
ReactInstanceManager reactInstanceManager = getReactNativeHost().getReactInstanceManager();
reactInstanceManager.addReactInstanceEventListener(new ReactInstanceEventListener() {
    @Override
    public void onReactContextInitialized(@NonNull ReactContext reactContext) {
        Log.v(LOG_TAG, "ReactApplicationContext initialized");
    }
});
```

This code should be run in `MainApplication.onCreate()`, but `ReactInstanceEventListener` is never invoked. To solve it
I had to create new custom `NativeModule` and register it with the application. See 
[FormDataModule](android/app/src/debug/java/com/simeonkirov/expodebug/multipart/FormDataModule.java)
and [FormDataPackage](android/app/src/debug/java/com/simeonkirov/expodebug/multipart/FormDataPackage.java). 
`FormDataModule` gets `ReactApplicationContext`, ensures that `ReactInstance` is initialized, gets the active 
`NetworkingModule` instance end register `FormDataRequestBodyHandler`
with it.

### 4. Create DebugReactNativeHost
By default, Expo creates this as nested class into `MainApplication`, but I prefer to have it separately. Use `getPackages()`
method to register `FormDataPackage`.

### 5. Create/update MainApplication class
For the debug project I created separate [DebugMainApplication](android/app/src/debug/java/com/simeonkirov/expodebug/DebugApplication.java)
class and registered it into [AndoridManifest.xml](android/app/src/debug/AndroidManifest.xml).

