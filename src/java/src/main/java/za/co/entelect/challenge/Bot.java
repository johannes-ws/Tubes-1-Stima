package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.*;
import static java.util.Collections.emptyList;

public class Bot {

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();
    private final static Command NOTHING = new DoNothingCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);
    private final static int RIGHT = 1;
    private final static int LEFT = -1;

    public Bot() {

    }

    public Command run(GameState gameState) {
        // Mengembalikan command yang akan dieksekusi pada suatu ronde di permainan

        // Setup blocks dan value yang akan dicek
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;

        int defaultFinalSpeed = getDefaultFinalSpeed(myCar);

        List<Lane> blocksInFront = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Lane> blocksInFrontLeft = getBlocksSide(myCar.position.lane, myCar.position.block, gameState, LEFT);
        List<Lane> blocksInFrontRight = getBlocksSide(myCar.position.lane, myCar.position.block, gameState, RIGHT);
        List<Lane> blocksInBack = getBlocksBack(myCar.position.lane, myCar.position.block, gameState);
        List<Lane> blocksIfAccelerate = blocksInFront.subList(0, min(speedIfAccelerate(defaultFinalSpeed, myCar.damage), blocksInFront.size()));
        List<Lane> blocksIfBoost = blocksInFront.subList(0, min(15, blocksInFront.size()));
        List<Lane> blocksIfNoAccelerate = blocksInFront.subList(0, min(defaultFinalSpeed, blocksInFront.size()));
        List<Lane> finalLizardBlock = blocksInFront.subList(max(0,min(defaultFinalSpeed - 1, blocksInFront.size())), min(defaultFinalSpeed, blocksInFront.size()));
        List<Lane> blocksIfLeft = emptyList();
        List<Lane> blocksIfRight = emptyList();

        if (myCar.position.lane != 1) {
            blocksIfLeft = blocksInFrontLeft.subList(0, min(defaultFinalSpeed, blocksInFrontLeft.size() - 1));
        }
        if (myCar.position.lane != 4) {
            blocksIfRight = blocksInFrontRight.subList(0, min(defaultFinalSpeed, blocksInFrontRight.size() - 1));
        }

        int leftFinalSpeed = finalSpeedIfCollide(blocksIfLeft, myCar, false);
        int centerFinalSpeed = finalSpeedIfCollide(blocksIfNoAccelerate, myCar, false);
        int accelerateFinalSpeed = finalSpeedIfCollide(blocksIfAccelerate, myCar, true);
        int rightFinalSpeed = finalSpeedIfCollide(blocksIfRight, myCar, false);
        int lizardFinalSpeed = finalSpeedIfCollide(finalLizardBlock, myCar, false);

        float powerUpsPrioPointsLeft = countPowerUpsPrioPoints(blocksIfLeft);
        float powerUpsPrioPointsCenter = countPowerUpsPrioPoints(blocksIfNoAccelerate);
        float powerUpsPrioPointsRight = countPowerUpsPrioPoints(blocksIfRight);

        boolean finalSpeedEqual = false;

        // STRATEGI BOT
        // Fix jika damage lebih dari 1
        if (myCar.damage > 1) {
            return FIX;
        }

        // Accelerate jika speed mobil 0
        if (defaultFinalSpeed == 0) {
            return ACCELERATE;
        }

        // GREEDY OBSTACLE AVOIDANCE
        // Dijalankan ketika ada obstacle atau mobil lawan yang akan ditabrak pemain jika tidak berbelok
        // Prioritas belok, menggunakan lizard, mencari jalur dengan speed akhir terbesar, mencari jalur dengan
        // point prioritas power up terbesar, ke lane tengah
        if (isObstaclePresent(blocksIfNoAccelerate) || isCollisionWithOpponentPossible(myCar, opponent, defaultFinalSpeed)) {
            // Belok kiri jika tidak ada obstacle yang menghalangi di kiri, ada obstacle di kanan dan mobil tidak di lane 1
            if (!isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight) && myCar.position.lane != 1) {
                return TURN_LEFT;
            }
            // Belok kanan jika tidak ada obstacle yang menghalangi di kanan, ada obstacle di kiri, dan mobil tidak di lane 4
            if (!isObstaclePresent(blocksIfRight) && isObstaclePresent(blocksIfLeft) && myCar.position.lane != 4) {
                return TURN_RIGHT;
            }
            // Jika kedua lane tidak ada obstacle, ke lane yang power up nya lebih banyak, lalu ke tengah
            if (!isObstaclePresent(blocksIfRight) && !isObstaclePresent(blocksIfLeft)) {
                if (myCar.position.lane == 1) {
                    return TURN_RIGHT;
                }
                if (myCar.position.lane == 4) {
                    return TURN_LEFT;
                }
                if (powerUpsPrioPointsLeft > powerUpsPrioPointsRight) {
                    return TURN_LEFT;
                }
                if (powerUpsPrioPointsLeft < powerUpsPrioPointsRight) {
                    return TURN_RIGHT;
                }
                // powerUpsPrioPoints di kiri dan kanan sama
                // Prioritaskan ke lane tengah
                if (myCar.position.lane == 2) {
                    return TURN_RIGHT;
                }
                if (myCar.position.lane == 3) {
                    return TURN_LEFT;
                }
            }

            // Ada obstacle di semua lane yang dapat dituju
            if ((isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight))
            || (myCar.position.lane == 1 && isObstaclePresent(blocksIfRight))
            || (myCar.position.lane == 4 && isObstaclePresent(blocksIfLeft))) {
                // Gunakan lizard jika punya dan speed akhir jika dipakai lebih besar dari tidak dipakai atau belok
                if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)
                        && lizardFinalSpeed > centerFinalSpeed
                        && (lizardFinalSpeed > leftFinalSpeed || myCar.position.lane == 1)
                        && (lizardFinalSpeed > rightFinalSpeed || myCar.position.lane == 4)) {
                    return LIZARD;
                }

                // Jika obstacle tidak bisa dihindari, cari yang final speednya lebih besar
                // Kalau harus belok dan final speednya sama, lihat dari point prioritas powerupnya
                                
                if (myCar.position.lane == 1) {
                    if (centerFinalSpeed < rightFinalSpeed) {
                        return TURN_RIGHT;
                    }
                    if (centerFinalSpeed == rightFinalSpeed) {
                        finalSpeedEqual = true;
                    }
                }
                else if (myCar.position.lane == 4) {
                    if (centerFinalSpeed < leftFinalSpeed) {
                        return TURN_LEFT;
                    }
                    if (centerFinalSpeed == leftFinalSpeed) {
                        finalSpeedEqual = true;
                    }
                }
                else {
                    if (leftFinalSpeed > centerFinalSpeed && leftFinalSpeed > rightFinalSpeed) {
                        return TURN_LEFT;
                    }
                    if (rightFinalSpeed > centerFinalSpeed && rightFinalSpeed > leftFinalSpeed) {
                        return TURN_RIGHT;
                    }
                    if (leftFinalSpeed == centerFinalSpeed && centerFinalSpeed > rightFinalSpeed) {
                        if (powerUpsPrioPointsLeft > powerUpsPrioPointsCenter) {
                            return TURN_LEFT;
                        }
                    }
                    if (rightFinalSpeed == centerFinalSpeed && centerFinalSpeed > leftFinalSpeed) {
                        if (powerUpsPrioPointsRight > powerUpsPrioPointsCenter) {
                            return TURN_RIGHT;
                        }
                    }
                    if (leftFinalSpeed == rightFinalSpeed && leftFinalSpeed > centerFinalSpeed) {
                        if (powerUpsPrioPointsLeft > powerUpsPrioPointsRight) {
                            return TURN_LEFT;
                        }
                        if (powerUpsPrioPointsLeft < powerUpsPrioPointsRight) {
                            return TURN_RIGHT;
                        }
                        if (myCar.position.lane == 2) {
                            return TURN_RIGHT;
                        }
                        if (myCar.position.lane == 3) {
                            return TURN_LEFT;
                        }
                    }
                    if (leftFinalSpeed == centerFinalSpeed && centerFinalSpeed == rightFinalSpeed) {
                        finalSpeedEqual = true;
                    }
                }
            }
        }

        // GREEDY AMBIL POWER UP
        // Tidak ada obstacle di tengah atau ada obstacle di semua arah dan final speed semua arah sama
        if (myCar.position.lane == 1) {
            if (!isObstaclePresent(blocksIfRight) || finalSpeedEqual) {
                if (powerUpsPrioPointsCenter < powerUpsPrioPointsRight) {
                    return TURN_RIGHT;
                }
            }
        }
        else if (myCar.position.lane == 4) {
            if (!isObstaclePresent(blocksIfLeft) || finalSpeedEqual) {
                if (powerUpsPrioPointsCenter < powerUpsPrioPointsLeft) {
                    return TURN_LEFT;
                }
            }
        }
        else if (!isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight)) {
            if (powerUpsPrioPointsCenter < powerUpsPrioPointsLeft) {
                return TURN_LEFT;
            }
        }
        else if (isObstaclePresent(blocksIfLeft) && !isObstaclePresent(blocksIfRight)) {
            if (powerUpsPrioPointsCenter < powerUpsPrioPointsRight) {
                return TURN_RIGHT;
            }
        }
        else if ((!isObstaclePresent(blocksIfLeft) && !isObstaclePresent(blocksIfRight)) || finalSpeedEqual) {
            if (powerUpsPrioPointsLeft > powerUpsPrioPointsCenter && powerUpsPrioPointsLeft > powerUpsPrioPointsRight) {
                return TURN_LEFT;
            }
            else if (powerUpsPrioPointsRight > powerUpsPrioPointsLeft && powerUpsPrioPointsRight > powerUpsPrioPointsCenter) {
                return TURN_RIGHT;
            }
            else if (powerUpsPrioPointsRight == powerUpsPrioPointsLeft && powerUpsPrioPointsRight > powerUpsPrioPointsCenter) {
                if (myCar.position.lane == 2) {
                    return TURN_RIGHT;
                }
                if (myCar.position.lane == 3) {
                    return TURN_LEFT;
                }
            }
        }
        
        // OBSTACLE PLACEMENT (EMP dan TWEET)
        // Gunakan EMP jika lawan ada di range EMP
        if (hasPowerUp(PowerUps.EMP, myCar.powerups) && isInEmpRange(myCar.position, opponent.position)) {
            return EMP;
        }

        // Gunakan TWEET di depan lawan jika punya TWEET
        if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
            return new TweetCommand(opponent.position.lane, opponent.position.block + speedIfAccelerate(opponent.speed, opponent.damage) + 1);
        }

        // BOOST
        // Gunakan BOOST jika tidak ada obstacle yang menghalangi, punya BOOST, dan tidak sedang memakai BOOST
        if (!isObstaclePresent(blocksIfBoost) && hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.boostCounter <= 1) {
            // Persiapan penggunaan BOOST jika ada damage
            if (myCar.damage == 1) {
                return FIX;
            }
            // BOOST digunakan saat tidak ada yang menghalangi dan damage 0
            if (!isCollisionWithOpponentPossible(myCar, opponent, maxSpeed(myCar.damage))) {
                return BOOST;
            }
        }

        // ACCELERATE
        // Gunakan ACCELERATE jika speed akan bertambah dan tidak ada obstacle atau speed akhir jika accelerate
        // lebih besar dari jika tidak menggunakan accelerate
        if (speedIfAccelerate(defaultFinalSpeed, myCar.damage) > defaultFinalSpeed
                && (!isObstaclePresent(blocksIfAccelerate)
                || accelerateFinalSpeed > centerFinalSpeed)) {
            return ACCELERATE;
        }

        // Gunakan OIL jika punya power up OIL, lawan di belakang, dan tidak ada obstacle di belakang
        if (hasPowerUp(PowerUps.OIL, myCar.powerups) && myCar.position.block > opponent.position.block && (!isObstaclePresent(blocksInBack) || myCar.position.lane == opponent.position.lane)) {
            return OIL;
        }

        // Coba ke lane tengah kalau tidak ada obstacle dan power up points sama atau lebih baik
        if (myCar.position.lane == 1 && !isObstaclePresent(blocksIfRight) && powerUpsPrioPointsRight >= powerUpsPrioPointsCenter) {
            return TURN_RIGHT;
        }

        if (myCar.position.lane == 4 && !isObstaclePresent(blocksIfLeft) && powerUpsPrioPointsLeft >= powerUpsPrioPointsCenter) {
            return TURN_LEFT;
        }

        return NOTHING;
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        // Mengembalikan true jika player memiliki powerup sesuai dengan powerUpToCheck
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }


    private List<Lane> getBlocksInFront(int lane, int block, GameState gameState) {
        // Mengembalikan block di depan mobil yang dapat terlihat di map
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock + 1, 0); i <= block - startBlock + 20; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i]);
        }
        return blocks;
    }

    private List<Lane> getBlocksSide(int lane, int block, GameState gameState, int direction) {
        // Mengembalikan block di samping kiri / kanan mobil yang terlihat di map
        if (lane + direction > 4 || lane + direction < 1) {
            return emptyList();
        }
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1 + direction);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + 20; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i]);
        }
        return blocks;
    }

    private List<Lane> getBlocksBack(int lane, int block, GameState gameState) {
        // Mengembalikan block di belakang mobil yang terlihat di map
        List<Lane[]> map = gameState.lanes;
        List<Lane> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock - 5, 0); i < block - startBlock; i++) {
            if (laneList[i] == null) {
                break;
            }

            blocks.add(laneList[i]);
        }
        return blocks;
    }

    private int speedIfAccelerate(int speed, int damage) {
        // Mengembalikan speed mobil jika diberikan command ACCELERATE
        int final_speed;
        switch (speed) {
            case 0:
                final_speed = 3;
                break;
            case 3:
            case 5:
                final_speed = 6;
                break;
            case 6:
                final_speed = 8;
                break;
            case 8:
            case 9:
                final_speed = 9;
                break;
            case 15:
                final_speed = 15;
                break;
            default: final_speed = 0;
        }

        return  min(final_speed, maxSpeed(damage));
    }

    private int maxSpeed(int damage) {
        // Mengembalikan kecepatan maksimum berdasarkan damage mobil
        switch (damage) {
            case 4: return 3;
            case 3: return 6;
            case 2: return 8;
            case 1: return 9;
            case 0: return 15;
            default: return 0;
        }
    }

    private int speedIfDecelerate(int speed) {
        // Mengembalikan speed mobil jika diberikan command DECELERATE
        switch (speed) {
            case 5:
            case 6: return 3;
            case 8: return 6;
            case 9: return 8;
            case 15: return 9;
            default: return 0;
        }
    }

    private Boolean isInEmpRange(Position myCarPosition, Position opponentPosition) {
        // Mengembalikan true jika mobil lawan berada di posisi yang akan terkena EMP
        if (myCarPosition.block < opponentPosition.block) {
            switch (myCarPosition.lane) {
                case 1:
                    if (opponentPosition.lane == 1 || opponentPosition.lane == 2) {
                        return true;
                    }
                    break;
                case 2:
                    if (opponentPosition.lane == 1 || opponentPosition.lane == 2 || opponentPosition.lane == 3) {
                        return true;
                    }
                    break;
                case 3:
                    if (opponentPosition.lane == 2 || opponentPosition.lane == 3 || opponentPosition.lane == 4) {
                        return true;
                    }
                    break;
                case 4:
                    if (opponentPosition.lane == 3 || opponentPosition.lane == 4) {
                        return true;
                    }
                    break;
                default: return false;
            }
        }
        return false;
    }

    private Boolean isObstaclePresent(List<Lane> blocks) {
        // Mengembalikan true jika ada obstacle di dalam list blocks
        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (terrain == Terrain.WALL || terrain == Terrain.MUD || terrain == Terrain.OIL_SPILL || block.isOccupiedByCyberTruck) {
                return true;
            }
        }
        return false;
    }

    private int finalSpeedIfCollide(List<Lane> blocks, Car myCar, boolean isAccelerating) {
        // Mengembalikan perhitungan kecepatan akhir jika melewati suatu lane
        int damage = 0;
        int speed_reduction = 0;
        int final_speed = -1;

        // Hitung total damage
        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (block.isOccupiedByCyberTruck) {
                return 0;
            }
            if (terrain == Terrain.WALL) {
                damage += 2;
            }
            else if (terrain == Terrain.MUD || terrain == Terrain.OIL_SPILL) {
                damage++;
            }
        }

        // Hitung total pengurangan speed
        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (terrain == Terrain.WALL) {
                final_speed = 3;
            }
            else if ((terrain == Terrain.MUD || terrain == Terrain.OIL_SPILL) && final_speed == -1) {
                speed_reduction += 1;
            }
        }

        // Hitung kecepatan akhir
        if (final_speed == -1) {
            if (isAccelerating) {
                final_speed = speedIfAccelerate(getDefaultFinalSpeed(myCar), myCar.damage);
            }
            else {
                final_speed = getDefaultFinalSpeed(myCar);
            }
            for (int i = 0; i < speed_reduction; i++) {
                final_speed = speedIfDecelerate(final_speed);
            }
            final_speed = max(3, final_speed);
        }

        return min(final_speed, maxSpeed(myCar.damage + damage));
    }

    private float countPowerUpsPrioPoints(List<Lane> blocks) {
        // Mengembalikan point prioritas powerup di dalam list blocks
        float powerUpsPrioPoints = 0;
        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            switch (terrain) {
                case LIZARD:
                    powerUpsPrioPoints += 2;
                    break;
                case BOOST:
                    powerUpsPrioPoints += 1.75;
                    break;
                case EMP:
                    powerUpsPrioPoints += 1.5;
                    break;
                case TWEET:
                    powerUpsPrioPoints += 1.25;
                    break;
                case OIL_POWER:
                    powerUpsPrioPoints++;
                    break;
                default:    
            }
        }
        return powerUpsPrioPoints;
    }

    private boolean isCollisionWithOpponentPossible(Car myCar, Car opponent, int speed) {
        // Mengembalikan true jika ada kemungkinan terjadi tabrakan dengan mobil lawan
        // Diasumsikan ada kemungkinan terjadi tabrakan jika ada di lane yang sama, mobil pemain di belakang lawan
        // dan block akhir pemain berada di depan block akhir lawan (jika tidak ada tabrakan)
        return myCar.position.lane == opponent.position.lane
                && myCar.position.block < opponent.position.block
                && myCar.position.block + getDefaultFinalSpeed(myCar) >= opponent.position.block + opponent.speed;
    }

    private int getDefaultFinalSpeed(Car myCar) {
        // Mengembalikan speed mobil untuk ronde ini jika tidak terjadi tabrakan dan tidak diberikan command boost atau accelerate
        if (myCar.speed == 15 && myCar.boostCounter <= 1 && myCar.boosting) {
            return 9;
        }
        else {
            return myCar.speed;
        }
    }
}
