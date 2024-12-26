import {Text, View, StyleSheet, SafeAreaView, Pressable} from "react-native";
import {Image} from "expo-image";
import * as DocumentPicker from 'expo-document-picker'
import {useState} from "react";

export default function Index() {
  const baseURL = process.env.EXPO_PUBLIC_BASE_URL;
  const pictureURI = process.env.EXPO_PUBLIC_PICTURE_URI;
  const pat = process.env.EXPO_PUBLIC_PAT;
  const url = `${baseURL}/${pictureURI}`;

  const [ loadedImage, setLoadedImage ] = useState<string>('')

  const uploadFileOnPressHandler = async () => {
    const pickerResult = await DocumentPicker.getDocumentAsync()

    if (!pickerResult.canceled && pickerResult.assets) {
      const form = new FormData()
      const asset = pickerResult.assets[0]
      form.append('file', asset)

      uploadPicture(form, asset.name)
    }
  }

  const uploadPicture = (form: FormData, assetName: string) => {
    const headers = {
      Authorization: `Bearer ${pat}`,
      'Content-Type': 'multipart/form-data',
    }
    const request = {
      method: 'POST',
      headers: headers,
      body: form,
    };
    fetch(url, request).then(response => {
      setLoadedImage(assetName)
    }).catch((error) => {
      console.log('!!!! Error: ', error)
    })
  }

  return (
    <SafeAreaView
      style={{
        flex: 1,
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Text style={styles.title}>Debugging expo network!</Text>
      <Pressable onPress={async () => uploadFileOnPressHandler()}>
        <Image
          transition={1000}
          contentFit="cover"
          source={{
            uri: `${url}?name=${loadedImage}`,
            headers: {
              Authorization: `Bearer ${pat}`,
            },
          }}
          style={styles.avatar}
          cachePolicy="none"
        />
      </Pressable>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  title: {
    fontWeight: 'bold',
    fontSize: 18,
    lineHeight: 30,
  },

  avatar: {
    width: 300,
    height: 300,
    borderRadius: 20,
    borderStyle: 'solid',
    borderColor: '#000',
  },
})
