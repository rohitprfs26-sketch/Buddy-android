# JARVIS Android — शुरुआती प्रोजेक्ट

यह एक शुरुआती native Android app project है। इसमें:
- Gemini API से Hindi/English AI chat (API key user खुद डालेगा)
- Android speech recognition से voice input
- Android Text-to-Speech से बोले हुए जवाब (फोन में उपलब्ध voice पर निर्भर)
- कुछ basic phone intents: YouTube, Camera, Settings, Alarm screen
- API key app preferences में इसी डिवाइस पर सेव होती है (यह encrypted vault नहीं है; फोन सुरक्षित रखें)

## ज़रूरी बातें
- यह APK अभी build करके test नहीं किया गया है। इसे शुरुआती source project समझें।
- Gemini API और Google की free-tier limits/availability बदल सकती हैं। Internet और अपनी API key ज़रूरी है।
- Voice recognition और Hindi/masculine voice आपके Android में installed speech/TTS engine पर निर्भर हैं। Pitch कम करने से वास्तविक deep male voice की गारंटी नहीं है।
- App अभी हमेशा सुनने वाला “Hey JARVIS” wake-word नहीं करता। Android में background microphone/wake-word के लिए अतिरिक्त permissions, foreground service और battery restrictions का काम चाहिए।
- AI से arbitrary phone controls नहीं कराए गए हैं। सिर्फ सुरक्षित, सीमित Android intents हैं।
- API key किसी के साथ share न करें। अगर key leak हो जाए तो Google AI Studio से revoke करें।

## Android APK कैसे बनाएं — मोबाइल से
1. GitHub पर account बनाएं/लॉगिन करें और नया **public** repository `jarvis-android` बनाएं।
2. इस ZIP की सभी files repository में upload करें (ZIP को पहले extract करना होगा)।
3. GitHub repository में **Actions** tab खोलें।
4. `Build JARVIS APK` workflow चुनें और **Run workflow** दबाएं।
5. Build पूरा होने पर run खोलें, नीचे **Artifacts** में `JARVIS-debug-APK` डाउनलोड करें।
6. ZIP artifact को extract करें; `app-debug.apk` मिलेगा। उसे Android में खोलकर install करें। Android पूछे तो browser/files app से install permission दें।

## Gemini API key
1. https://aistudio.google.com/ खोलें।
2. अपनी Google account से sign in करके API key बनाएं।
3. JARVIS app खोलें → **GEMINI API KEY** → key paste → Save.
4. API key private रखें। इसे chat या public GitHub repo में न डालें।

## अभी उपलब्ध commands
- “Open YouTube” / “यूट्यूब खोलो”
- “Open camera” / “कैमरा खोलो”
- “Open settings” / “सेटिंग खोलो”
- “Set alarm” / “अलार्म लगाओ” (अलार्म स्क्रीन खुलेगी; समय खुद चुनें)

## Limitations
यह version background wake word, WhatsApp message sending, calls, arbitrary device control, offline large language model, cloud sync, and Play Store publishing शामिल नहीं करता। इन सुविधाओं के लिए अलग implementation और permissions/approvals चाहिए।
