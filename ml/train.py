"""
Pelatihan model deteksi penyakit padi (AgriBicara Fase 4, Task 0).

Melatih klasifikator MobileNetV3 (transfer learning) di dataset Paddy Doctor,
mengevaluasinya, lalu mengekspor DUA berkas yang di-BUNDEL ke aplikasi
(Firebase ML Model Hosting sudah deprecated, jadi model tidak diunggah ke
Firebase melainkan disalin ke assets):
  - rice_disease_classifier.tflite  -> app/src/main/assets/
  - disease_labels.txt              -> app/src/main/assets/

KONTRAK PRA-PROSES — WAJIB cocok dengan aplikasi:
  Model dibangun dengan MobileNetV3(include_preprocessing=True), sehingga ia
  MENUNGGU input piksel MENTAH [0..255] float32 dan menormalkannya sendiri.
  Aplikasi mengirim piksel mentah [0..255] (Constants.DISEASE_INPUT_MEAN=0,
  STD=1). Jangan ubah salah satunya tanpa mengubah yang lain — mismatch =
  prediksi yakin-tapi-ngawur.

KONTRAK LABEL:
  disease_labels.txt berisi satu label per baris, URUTANNYA = urutan output
  model (class_names dari image_dataset_from_directory, alfabetis). Nama label
  harus cocok dengan kunci di app `domain/ml/DiseaseCatalog.kt`
  (mis. blast, brown_spot, normal). Label di luar katalog otomatis jatuh ke
  "belum yakin" di aplikasi — aman, tapi tak ada terjemahannya.

RIWAYAT — kenapa resep ini seperti sekarang:
  Resep pertama (backbone BEKU 12 epoch, lalu hanya 30 lapisan teratas dengan
  LR 1e-5 selama 6 epoch) hanya mencapai val_accuracy 0.586 dengan loss 1.211
  (tebakan acak 10 kelas = 2.303). Itu UNDERFIT: fitur ImageNet beku tidak
  cukup untuk membedakan tekstur bercak daun yang mirip, dan fine-tune-nya
  terlalu lembut untuk menggesernya. Kelas terkecil (bacterial_leaf_blight)
  jatuh ke akurasi 0.04 karena tidak ada penyeimbang kelas. Perbaikannya:
    1. Tahap 2 membuka SELURUH backbone dengan LR 1e-4 (cosine decay).
    2. EarlyStopping + restore_best_weights -> yang diekspor bobot TERBAIK,
       bukan epoch terakhir.
    3. class_weight menyeimbangkan kelas kecil.
    4. Ambang keyakinan DIUKUR dari distribusi val, bukan ditebak.

Lisensi dataset: Paddy Doctor — Apache License 2.0
  (github.com/paddydoc/paddy-doctor-dataset). Lihat ml/NOTICE untuk atribusi
  yang WAJIB ikut disertakan pada rilis.

Contoh:
  python train.py --data_dir ./paddy_data/train_images
  python train.py --data_dir ./paddy_data/train_images --backbone large
"""

import argparse
import json
import os

import numpy as np
import tensorflow as tf

IMG_SIZE = 224            # WAJIB sama dengan Constants.DISEASE_MODEL_INPUT_SIZE
BATCH_SIZE = 32
SEED = 1337
MODEL_NAME = "rice_disease_classifier"   # -> {MODEL_NAME}.tflite = Constants.DISEASE_MODEL_ASSET


def build_datasets(data_dir):
    """train/val split 80/20 dari folder berisi subfolder per kelas."""
    common = dict(
        validation_split=0.2,
        seed=SEED,
        image_size=(IMG_SIZE, IMG_SIZE),
        batch_size=BATCH_SIZE,
        label_mode="int",
    )
    train_ds = tf.keras.utils.image_dataset_from_directory(data_dir, subset="training", **common)
    val_ds = tf.keras.utils.image_dataset_from_directory(data_dir, subset="validation", **common)
    class_names = train_ds.class_names
    autotune = tf.data.AUTOTUNE
    # image_dataset_from_directory sudah mengeluarkan float32 [0..255] — JANGAN
    # dibagi 255: MobileNetV3(include_preprocessing=True) menormalkan sendiri.
    train_ds = train_ds.cache().shuffle(2000, seed=SEED).prefetch(autotune)
    val_ds = val_ds.cache().prefetch(autotune)
    return train_ds, val_ds, class_names


def compute_class_weights(data_dir, class_names):
    """
    Bobot kelas dari jumlah berkas per folder (rumus 'balanced' sklearn).

    Tanpa ini kelas terkecil tenggelam: pada latihan pertama
    bacterial_leaf_blight (paling sedikit) hanya mencapai akurasi 0.04 —
    model belajar mengabaikannya karena secara loss itu murah.
    """
    counts = []
    for name in class_names:
        folder = os.path.join(data_dir, name)
        counts.append(sum(1 for f in os.listdir(folder) if not f.startswith(".")))
    total = float(sum(counts))
    n = len(counts)
    weights = {i: total / (n * c) for i, c in enumerate(counts)}
    print("Jumlah gambar per kelas:")
    for i, (name, c) in enumerate(zip(class_names, counts)):
        print(f"  {name:<28} {c:>5}  bobot={weights[i]:.2f}")
    return weights


def build_model(num_classes, backbone):
    data_augmentation = tf.keras.Sequential(
        [
            tf.keras.layers.RandomFlip("horizontal_and_vertical"),
            tf.keras.layers.RandomRotation(0.15),
            tf.keras.layers.RandomZoom(0.15),
            tf.keras.layers.RandomContrast(0.15),
        ],
        name="augment",
    )
    factory = (
        tf.keras.applications.MobileNetV3Large
        if backbone == "large"
        else tf.keras.applications.MobileNetV3Small
    )
    base = factory(
        input_shape=(IMG_SIZE, IMG_SIZE, 3),
        include_top=False,
        weights="imagenet",
        include_preprocessing=True,   # model menunggu input MENTAH [0..255]
    )
    base.trainable = False

    inputs = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
    x = data_augmentation(inputs)
    # training=False menahan BatchNorm di mode inferensi SELAMANYA, termasuk saat
    # fine-tune. Ini pola resmi Keras: memperbarui statistik BN dengan data
    # sebesar ini bisa merusak bobot pra-latih yang justru ingin kita pakai.
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax")(x)
    return tf.keras.Model(inputs, outputs), base


def evaluate_per_class(model, val_ds, class_names):
    """Akurasi per kelas + keyakinan tiap prediksi, dalam SATU lintasan val."""
    probs, truths = [], []
    for images, labels in val_ds:
        probs.append(model.predict(images, verbose=0))
        truths.append(labels.numpy())
    probs = np.concatenate(probs)
    truths = np.concatenate(truths)
    preds = probs.argmax(axis=1)
    confid = probs.max(axis=1)
    correct = preds == truths

    print(f"\nAkurasi validasi keseluruhan: {correct.mean():.3f}")
    print("Akurasi per kelas:")
    for i, name in enumerate(class_names):
        mask = truths == i
        acc = correct[mask].mean() if mask.any() else float("nan")
        print(f"  {name:<28} {acc:.2f}  ({int(mask.sum())} gambar)")
    return confid, correct


def recommend_threshold(confid, correct):
    """
    Menyetel DISEASE_CONFIDENCE_THRESHOLD dari ANGKA, bukan tebakan.

    Aplikasi hanya menampilkan dugaan bila keyakinan >= ambang; di bawahnya
    "belum yakin". Yang dicari: ambang TERENDAH yang membuat dugaan yang
    DITAMPILKAN benar minimal 90% — petani bisa salah beli pestisida kalau
    aplikasi menampilkan tebakan yang terlihat yakin.
    """
    print("\nKalibrasi ambang (dari data validasi):")
    print(f"  {'ambang':>8} {'ditampilkan':>12} {'benar bila ditampilkan':>24}")
    pilihan = None
    for step in range(8, 20):                      # 0.40 .. 0.95
        t = round(step * 0.05, 2)
        shown = confid >= t
        if not shown.any():
            continue
        precision = correct[shown].mean()
        coverage = shown.mean()
        print(f"  {t:>8.2f} {coverage:>11.1%} {precision:>23.1%}")
        if pilihan is None and precision >= 0.90:
            pilihan = (t, coverage, precision)
    if pilihan:
        t, coverage, precision = pilihan
        print(
            f"\n=> Setel Constants.DISEASE_CONFIDENCE_THRESHOLD = {t:.2f}f\n"
            f"   Pada ambang itu {coverage:.0%} foto mendapat dugaan, "
            f"dan {precision:.0%} di antaranya benar."
        )
    else:
        print(
            "\n=> TIDAK ADA ambang yang mencapai 90% ketepatan. Model belum layak\n"
            "   dipakai mendiagnosis; latih lagi (coba --backbone large) sebelum\n"
            "   memasang model ini ke aplikasi."
        )
    print("\nCATATAN: akurasi lapangan (foto HP) hampir selalu LEBIH RENDAH dari val.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data_dir", required=True, help="Folder berisi subfolder per kelas")
    parser.add_argument("--backbone", choices=["small", "large"], default="small",
                        help="small = APK kecil & cepat (default); large = lebih akurat, model ~3x lebih besar")
    parser.add_argument("--warmup_epochs", type=int, default=5,
                        help="Tahap 1: latih kepala saja di atas backbone beku")
    parser.add_argument("--fine_tune_epochs", type=int, default=25,
                        help="Tahap 2: seluruh backbone terbuka (EarlyStopping biasanya berhenti lebih awal)")
    parser.add_argument("--fine_tune_lr", type=float, default=1e-4)
    parser.add_argument("--out_dir", default="./out")
    args = parser.parse_args()

    os.makedirs(args.out_dir, exist_ok=True)
    train_ds, val_ds, class_names = build_datasets(args.data_dir)
    print(f"Kelas ({len(class_names)}): {class_names}")
    class_weight = compute_class_weights(args.data_dir, class_names)

    model, base = build_model(len(class_names), args.backbone)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )

    print("\n== Tahap 1: latih kepala klasifikasi (backbone beku) ==")
    model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=args.warmup_epochs,
        class_weight=class_weight,
    )

    # Tahap 2 — SELURUH backbone dibuka. Resep lama hanya membuka 30 lapisan
    # teratas dengan LR 1e-5, terlalu lembut: val_accuracy mentok di 0.586.
    print("\n== Tahap 2: fine-tune SELURUH backbone ==")
    base.trainable = True
    steps_per_epoch = int(train_ds.cardinality().numpy())
    schedule = tf.keras.optimizers.schedules.CosineDecay(
        initial_learning_rate=args.fine_tune_lr,
        decay_steps=max(1, steps_per_epoch * args.fine_tune_epochs),
    )
    model.compile(
        optimizer=tf.keras.optimizers.Adam(schedule),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=args.fine_tune_epochs,
        class_weight=class_weight,
        callbacks=[
            # restore_best_weights: yang diekspor adalah bobot TERBAIK. Resep
            # lama mengekspor epoch terakhir — kalau epoch terakhir lebih buruk,
            # model yang lebih jelek itulah yang sampai ke petani.
            tf.keras.callbacks.EarlyStopping(
                monitor="val_accuracy",
                patience=5,
                restore_best_weights=True,
                verbose=1,
            ),
        ],
    )

    confid, correct = evaluate_per_class(model, val_ds, class_names)
    recommend_threshold(confid, correct)

    # --- Ekspor label (urutan = output model) ---
    labels_path = os.path.join(args.out_dir, "disease_labels.txt")
    with open(labels_path, "w", encoding="utf-8") as f:
        f.write("\n".join(class_names) + "\n")
    print(f"\nLabel ditulis: {labels_path}")

    # --- Konversi TFLite (dynamic-range quant: kecil, input tetap float32) ---
    # CATATAN VERSI: konverter TF baru memancarkan FULLY_CONNECTED v12, yang
    # BUTUH runtime tensorflow-lite >= 2.17.0 di aplikasi (2.16.1 gagal memuat
    # dengan "Didn't find op for builtin opcode"). Lihat gradle/libs.versions.toml.
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    tflite_path = os.path.join(args.out_dir, f"{MODEL_NAME}.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"Model TFLite ditulis: {tflite_path} ({len(tflite_model) / 1e6:.2f} MB)")

    with open(os.path.join(args.out_dir, "metrics.json"), "w", encoding="utf-8") as f:
        json.dump(
            {
                "backbone": args.backbone,
                "class_names": class_names,
                "val_accuracy": float(correct.mean()),
                "tf_version": tf.__version__,
            },
            f,
            indent=2,
        )

    print(
        "\nLangkah berikutnya (model DI-BUNDEL di app, bukan Firebase):\n"
        f"  1) Salin {labels_path} -> app/src/main/assets/disease_labels.txt\n"
        f"  2) Salin {tflite_path} -> app/src/main/assets/{MODEL_NAME}.tflite\n"
        "  3) Setel Constants.DISEASE_CONFIDENCE_THRESHOLD dari kalibrasi di atas\n"
        "  4) ./gradlew assembleDebug && adb install -r  (JANGAN pakai Apply Changes)\n"
    )


if __name__ == "__main__":
    main()
