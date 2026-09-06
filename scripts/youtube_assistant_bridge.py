# -*- coding: utf-8 -*-
"
HatırlaGit - Jarvis / Usta YouTube & Google Asistan Python Köprüsü
Bu script; bilgisayarınızda veya telefonunuzdaki Termux/Pydroid ortamında
sesli komutla YouTube'da istediğiniz şarkıyı açmanızı ve Google Asistan ile köprü kurmanızı sağlar.

Gereksinimler:
pip install pywhatkit SpeechRecognition pyttsx3
"

import sys
import webbrowser
import urllib.parse

def play_youtube_song(song_name: str):
    "
    YouTube'da belirtilen şarkıyı veya videoyu doğrudan açar ve oynatır.
    "
    print(f🎵 Jarvis / Usta: '{song_name}' parçası YouTube üzerinde açılıyor...)
    try:
        import pywhatkit
        pywhatkit.playonyt(song_name)
        print(✅ Şarkı pywhatkit ile başlatıldı.)
    except Exception as e:
        # Fallback: Doğrudan web tarayıcısı üzerinden YouTube araması
        encoded = urllib.parse.quote_plus(song_name)
        url = fhttps://www.youtube.com/results?search_query={encoded}
        webbrowser.open(url)
        print(f✅ Şarkı tarayıcıda açıldı: {url})

def trigger_google_assistant():
    "
    Google Asistan veya web arayüzü ile köprü kurar.
    "
    print(🎙️ Google Asistan Köprüsü tetikleniyor...)
    webbrowser.open(https://assistant.google.com/)

def main():
    if len(sys.argv) > 1:
        query =  .join(sys.argv[1:])
        play_youtube_song(query)
    else:
        print(Jarvis YouTube & Sesli Köprü Sistemi Aktif!)
        query = input(Çalmak istediğiniz şarkının veya sanatçının adını yazın: )
        if query.strip():
            play_youtube_song(query.strip())

if __name__ == __main__:
    main()
