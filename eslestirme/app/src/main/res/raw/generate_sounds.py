import wave
import struct
import math

def generate_sound(filename, frequency, duration, volume=0.5, type="sine"):
    sample_rate = 44100.0
    num_samples = int(duration * sample_rate)

    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1) # mono
        wav_file.setsampwidth(2) # 2 bytes per sample
        wav_file.setframerate(sample_rate)

        for i in range(num_samples):
            t = float(i) / sample_rate
            if type == "sine":
                value = math.sin(2.0 * math.pi * frequency * t)
            elif type == "square":
                value = 1.0 if math.sin(2.0 * math.pi * frequency * t) > 0 else -1.0
            elif type == "triangle":
                value = 2.0 * abs(2.0 * (t * frequency - math.floor(t * frequency + 0.5))) - 1.0
            else:
                value = 0.0

            # Envelope (fade out)
            envelope = 1.0 - (float(i) / num_samples)
            value = value * volume * envelope

            packed_value = struct.pack('h', int(value * 32767.0))
            wav_file.writeframes(packed_value)

generate_sound("match.wav", frequency=880.0, duration=0.4, type="sine")
generate_sound("mismatch.wav", frequency=220.0, duration=0.4, type="square")
generate_sound("combo.wav", frequency=1200.0, duration=0.5, type="sine")
generate_sound("gameover.wav", frequency=150.0, duration=1.0, type="square")
generate_sound("boss_hit.wav", frequency=300.0, duration=0.3, type="triangle")
generate_sound("ice_break.wav", frequency=2000.0, duration=0.2, type="triangle")
generate_sound("bgm.wav", frequency=440.0, duration=3.0, type="sine", volume=0.1) # Basit bgm
