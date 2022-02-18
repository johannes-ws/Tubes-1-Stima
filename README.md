# Tubes-1-Stima

#### Pemanfaatan Algoritma Greedy dalam Permainan Overdrive

## Spesifikasi Permainan

1. Overdrive adalah sebuah game yang mempertandingan 2 bot mobil dalam sebuah ajang balapan.
2. Pemenang dari permainan ini adalah player pertama yang melewati blok finish.
3. Terdapat command-command dan powerups yang dapat dimanfaatkan di dalam permainan.
4. Memanfaatkan Algoritma Greedy dalam Permainan Overdrive dengan membuat bot.
5. Code Algoritma dibuat dengan melanjutkan code awal yang telah disiapkan oleh developer.

## Strategi Greedy yang Digunakan

1.  Fix jika memiliki damage lebih dari 1
    Alasannya agar memiliki max speed sebesar 9. Alasan kami tidak memilih damage lebih dari 0 karena setiap kali fix akan mengurangi damage sebesar 2, jadi rugi kalau masih memiliki 1 damage.

2.  Accelerate jika speed sama dengan 0
    Alasannya hanya sekadar untuk berjaga-jaga jikalau mobil tersebut berhenti.

3.  Belok kiri ataupun kanan jika ada obstacle ataupun musuh di depan
    Alasannya agar bisa meminimalkan damage yang diterima ataupun agar mobil kami tidak stuck di belakang mobil musuh.

            3.1 Gunakan lizard jika terdapat obstacle di depan, kiri, dan kanan mobil
            Alasannya sama seperti poin sebelumnya, yaitu meminimalkan damage yang diterima.

            3.2 Pilih lane dimana final speed setelah menabrak obstacle merupakan yang terbesar
            Alasannya karena tidak ada jalan lain selain terus maju. Oleh karena itu, kami mempertimbangkan hal itu dengan memilih lane yang memiliki final speed yang terbesar, agar bisa memaksimalkan kecepatan setelah menabrak obstacle.

            3.3 Pilih lane dimana total poin prioritas powerup merupakan yang terbesar
            Alasannya karena kami ingin memaksimalkan langkah ketika menentukan lane yang terbaik. Kami mendefinisikan lizard senilai 2 poin, emp senilai 1.75 poin, boost senilai 1.5 poin, tweet senilai 1.25 poin, dan yang terakhir oil senilai 1 poin. Kami memilih angka-angka tersebut karena disesuaikan dengan pertimbangan kami. Menurut kami, lizard merupakan powerup yang paling penting karena bisa dipakai saat tertinggal maupun saat memimpin. Powerup emp kami prioritaskan kedua sebagai amunisi jika mobil kami tertinggal dengan mobil musuh. Powerup boost kami prioritaskan ketiga karena tidak mudah untuk menggunakan boost, belum lagi jika musuh menggunakan emp. Keempat ada powerup tweet agar bisa memaksa musuh untuk berbelok di saat kami memimpin ataupun tertinggal. Terakhir ada powerup oil karena powerup ini memiliki chance terkena musuh yang sangat kecil.

4.  Gunakan emp di saat musuh berada di depan, kiri, atau kanan
    Alasannya untuk mengejar ketertinggalan dari musuh. Kami juga memprioritaskan powerup emp dahulu agar membuat musuh tidak tenang dalam berkendara.

5.  Gunakan tweet di lane musuh dan blok dimana musuh berhenti saat accelerate
    Alasannya agar mengganggu musuh di mana pun dan kapan pun selagi bisa. Kami meletakkan tweet di lane musuh berada dan blok dimana dia berhenti saat melakukan accelerate plus satu agar memaksimalkan kemungkinan memaksa musuh untuk berbelok.

6.  Fix sebelum memanggil command boost jika damage sama dengan 1
    Alasannya agar memaksimalkan kecepatan dari boost itu sendiri. Kami berpikir bahwa sia-sia jika menggunakan boost apabila max speed yang dimiliki tidak sama dengan kecepatan boost.

7.  Gunakan boost
    Alasannya agar mengejar ketertinggalan dari musuh ataupun semakin menjauh dari musuh. Kami menggunakan command boost terlebih dahulu sebelum accelerate karena kami ingin memaksimalkan kecepatan setelah boost tersebut habis.

8.  Accelerate apabila speed akan bertambah
    Alasannya agar memiliki kecepatan yang konstan di state max speed.

9.  Gunakan oil jika musuh berada di belakang
    Alasannya untuk mengganggu pergerakan musuh, sehingga dia akan terpaksa menghindari obstacle tersebut. Powerup oil kami prioritaskan terakhir karena kami merasa bahwa oil memiliki chance yang kecil untuk mengenai musuh.

10. Belok ke lane 2 atau 3 jika berada di pinggir lintasan
    Alasannya agar di round selanjutnya bisa memiliki dua pilihan belok. Selain itu, alasan kami ingin bergerak ke tengah agar memperbesar kemungkinan pemanggilan command emp untuk menyerang musuh di saat kami tertinggal.

## Requirement

1. Official Overdrive Game Engine: https://github.com/EntelectChallenge/2020-Overdrive/releases/tag/2020.3.4
2. Java (minimal Java 8): https://www.oracle.com/java/technologies/downloads/#java8
3. IntelIiJ IDEA: https://www.jetbrains.com/idea/
4. NodeJS: https://nodejs.org/en/download/
5. Visualizer (Optional) : https://entelect-replay.raezor.co.za/

## Build Program

1. Pastikan terlebih dahulu requirement sudah terinstall semua.
2. Dalam folder starter-pack terdapat folder-folder, tetapi tenang untuk sekarang perhatikan salah satu folder bot, misal starter-bot
3. Dalam folder tersebut terdapat file pom.xml yang berguna untuk mengenali project java di Intellij IDEA.
4. Pada Maven Toolbox di Intellij IDEA, tambahkan project java ini jika misalnya belum terindentifikasi dengan menggunakan tombol + dan memilih file pom.xml tadi
5. Kemudian Build project ini dengan menggunakan Maven Toolbox pada bagian Lifecycle dan kemudian install
6. Akan terbentuk folder bernama target yang isinya berisi file berekstensi .jar

## Menjalankan Permainan

1. Copy file .jar yang ada di folder bin ke tempat yang akan digunakan
2. Copy juga source code program dengan menggunakan src pada repositori ini
3. Perhatikan konfigurasi yang ada di game-runner-config.json terutama berkaitan dengan direktori bot yang digunakan
4. Jika konfigurasi sudah dilakukan, jalankan run.bat
5. Akan tampil permainan pada command prompt dan rekap pertandingan akan tersimpan di folder match-logs
6. Jika ingin menggunakan visualizer yang kami sarankan di bagian requirement, zip folder pertandingan yang ada di folder match-logs
7. Klik link visualizernya
8. Masukkan file zip tadi ke visualizer dengan menggunakan tanda + di bagian atas kiri
9. Kemudian permainan dapat di eksplorasi dengan visualizer ini
