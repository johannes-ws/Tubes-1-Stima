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
    private final static Command DECELERATE = new DecelerateCommand();
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
        // SETUP BLOCKS DAN VALUE YANG AKAN DICEK
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;

        List<Lane> blocksInFront = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Lane> blocksInFrontLeft = getBlocksSide(myCar.position.lane, myCar.position.block, gameState, LEFT);
        List<Lane> blocksInFrontRight = getBlocksSide(myCar.position.lane, myCar.position.block, gameState, RIGHT);
        List<Lane> blocksInBack = getBlocksBack(myCar.position.lane, myCar.position.block, gameState);
        List<Lane> blocksIfAccelerate = blocksInFront.subList(0, min(speedIfAccelerate(myCar.speed, myCar.damage), blocksInFront.size()));
        List<Lane> blocksIfBoost = blocksInFront.subList(0, min(15, blocksInFront.size()));
        List<Lane> blocksIfNoAccelerate = blocksInFront.subList(0, min(myCar.speed, blocksInFront.size()));
        List<Lane> blocksIfLeft = emptyList();
        List<Lane> blocksIfRight = emptyList();

        if (myCar.position.lane != 1) {
            blocksIfLeft = blocksInFrontLeft.subList(0, min(myCar.speed, blocksInFrontLeft.size() - 1));
        }
        if (myCar.position.lane != 4) {
            blocksIfRight = blocksInFrontRight.subList(0, min(myCar.speed, blocksInFrontRight.size() - 1));
        }

        int leftFinalSpeed = finalSpeedIfCollide(blocksIfLeft, myCar);
        int centerFinalSpeed = finalSpeedIfCollide(blocksIfNoAccelerate, myCar);
        int rightFinalSpeed = finalSpeedIfCollide(blocksIfRight, myCar);

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
        if (myCar.speed == 0) {
            return ACCELERATE;
        }

        // Salip musuh
        if (myCar.position.lane == opponent.position.lane
                && myCar.position.block < opponent.position.block
                && myCar.position.block + speedIfBoost(myCar.damage) >= opponent.position.block + opponent.speed) {
            if (myCar.position.lane != 1 && !isObstaclePresent(blocksIfLeft)) {
                return TURN_LEFT;
            }
            if (myCar.position.lane != 4 && !isObstaclePresent(blocksIfRight)) {
                return TURN_RIGHT;
            }
        }

        // GREEDY OBSTACLE AVOIDANCE
        // Hindari obstacle, gunakan lizard, cari jalur yang speed akhirnya paling besar, cari jalur yang dapat memberikan point prioritas powerup terbanyak
        if (isObstaclePresent(blocksIfNoAccelerate)) {
            // Belok kiri jika tidak ada obstacle yang menghalangi di kiri, ada obstacle di kanan dan mobil tidak di lane 1
            if (!isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight) && myCar.position.lane != 1) {
                return TURN_LEFT;
            }
            // Belok kanan jika tidak ada obstacle yang menghalangi di kanan, ada obstacle di kiri, dan mobil tidak di lane 4
            if (!isObstaclePresent(blocksIfRight) && isObstaclePresent(blocksIfLeft) && myCar.position.lane != 4) {
                return TURN_RIGHT;
            }
            // Jika kedua lane tidak ada obstacle, ke lane yang power up nya lebih banyak
            if (!isObstaclePresent(blocksIfRight) && !isObstaclePresent(blocksIfLeft)) {
                if (myCar.position.lane == 1) {
                    return TURN_RIGHT;
                }
                if (myCar.position.lane == 4) {
                    return TURN_LEFT;
                }
                if (countPowerUpsPrioPoints(blocksIfLeft) > countPowerUpsPrioPoints(blocksIfRight)) {
                    return TURN_LEFT;
                }
                if (countPowerUpsPrioPoints(blocksIfLeft) < countPowerUpsPrioPoints(blocksIfRight)) {
                    return TURN_RIGHT;
                }
                return TURN_RIGHT;
            }

            // Lizard jika kedua lane ada obstacle, punya lizard
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                if (myCar.position.lane == 1 && isObstaclePresent(blocksIfRight)) {
                    return LIZARD;
                }
                else if (myCar.position.lane == 4 && isObstaclePresent(blocksIfLeft)) {
                    return LIZARD;
                }
                else if (isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight)) {
                    return LIZARD;
                }
            }

            // Ada obstacle di semua lane yang dapat dituju
            if ((isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight))
            || (myCar.position.lane == 1 && isObstaclePresent(blocksIfRight))
            || (myCar.position.lane == 4 && isObstaclePresent(blocksIfLeft))) {
                // Jika kedua lane ada obstacle, cari yang final speednya lebih besar
                // Kalau harus belok, lihat dari point prioritas powerupnya
                                
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
                        if (countPowerUpsPrioPoints(blocksIfLeft) > countPowerUpsPrioPoints(blocksIfNoAccelerate)) {
                            return TURN_LEFT;
                        }
                    }
                    if (rightFinalSpeed == centerFinalSpeed && centerFinalSpeed > leftFinalSpeed) {
                        if (countPowerUpsPrioPoints(blocksIfRight) > countPowerUpsPrioPoints(blocksIfNoAccelerate)) {
                            return TURN_RIGHT;
                        }
                    }
                    if (leftFinalSpeed == rightFinalSpeed && leftFinalSpeed > centerFinalSpeed) {
                        if (countPowerUpsPrioPoints(blocksIfLeft) > countPowerUpsPrioPoints(blocksIfRight)) {
                            return TURN_LEFT;
                        }
                        if (countPowerUpsPrioPoints(blocksIfLeft) < countPowerUpsPrioPoints(blocksIfRight)) {
                            return TURN_RIGHT;
                        }
                        return TURN_RIGHT;
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
                if (powerUpsPrioPointsCenter < powerUpsPrioPointsRight && powerUpsPrioPointsRight >= 1) {
                    return TURN_RIGHT;
                }
            }
        }
        else if (myCar.position.lane == 4) {
            if (!isObstaclePresent(blocksIfLeft) || finalSpeedEqual) {
                if (powerUpsPrioPointsCenter < powerUpsPrioPointsLeft && powerUpsPrioPointsLeft >= 1) {
                    return TURN_LEFT;
                }
            }
        }
        else if (!isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight)) {
            if (powerUpsPrioPointsCenter < powerUpsPrioPointsLeft && powerUpsPrioPointsLeft >= 1) {
                return TURN_LEFT;
            }
        }
        else if (isObstaclePresent(blocksIfLeft) && !isObstaclePresent(blocksIfRight)) {
            if (powerUpsPrioPointsCenter < powerUpsPrioPointsRight && powerUpsPrioPointsRight >= 1) {
                return TURN_RIGHT;
            }
        }
        else if ((!isObstaclePresent(blocksIfLeft) && !isObstaclePresent(blocksIfRight)) || finalSpeedEqual) {
            if (powerUpsPrioPointsLeft > powerUpsPrioPointsCenter && powerUpsPrioPointsLeft > powerUpsPrioPointsRight && powerUpsPrioPointsLeft >= 1) {
                return TURN_LEFT;
            }
            else if (powerUpsPrioPointsRight > powerUpsPrioPointsLeft && powerUpsPrioPointsRight > powerUpsPrioPointsCenter && powerUpsPrioPointsRight >= 1) {
                return TURN_RIGHT;
            }
            else if (powerUpsPrioPointsRight == powerUpsPrioPointsLeft && powerUpsPrioPointsRight > powerUpsPrioPointsCenter && powerUpsPrioPointsRight >= 1) {
                return TURN_RIGHT;
            }
        }

        
        // OBSTACLE PLACEMENT (EMP dan TWEET)
        // Gunkaan emp jika lawan di depan dan lawan ada di lane yang ada di range emp
        if (hasPowerUp(PowerUps.EMP, myCar.powerups) && isInEmpRange(myCar.position, opponent.position)) {
            return EMP;
        }

        // Gunakan tweet di depan lawan jika punya tweet
        if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
            return new TweetCommand(opponent.position.lane, opponent.position.block + speedIfAccelerate(opponent.speed, opponent.damage) + 1);
        }

        // BOOST
        // Persiapan boost
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && !isObstaclePresent(blocksIfBoost) && myCar.damage == 1) {
            return FIX;
        }

        // Boost jika speed akan bertambah saat command diberikan, tidak ada obstacle yang menghalangi, dan boost tersedia
        if (myCar.speed < speedIfBoost(myCar.damage) && !isObstaclePresent(blocksIfBoost) && hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting) {
            return BOOST;
        }

        // ACCELERATE
        // Accelerate jika speed akan bertambah dan tidak ada obstacle
        if (speedIfAccelerate(myCar.speed, myCar.damage) > myCar.speed && !isObstaclePresent(blocksIfAccelerate) && !myCar.boosting) {
            return ACCELERATE;
        }

        // Gunakan oil jika lawan ada di belakang dan di belakang tidak ada obstacle
        if (hasPowerUp(PowerUps.OIL, myCar.powerups) && myCar.position.block > opponent.position.block && (!isObstaclePresent(blocksInBack) || myCar.position.lane == opponent.position.lane)) {
            return OIL;
        }

        // Coba ke lane tengah kalau tidak ada obstacle
        if (myCar.position.lane == 1 && !isObstaclePresent(blocksIfRight)) {
            return TURN_RIGHT;
        }

        if (myCar.position.lane == 4 && !isObstaclePresent(blocksIfLeft)) {
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

    private int speedIfBoost(int damage) {
        // Mengembalikan speed jika diberikan command BOOST
        switch (damage) {
            case 4: return 3;
            case 3: return 6;
            case 2: return 8;
            case 1: return 9;
            case 0: return 15;
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

    private int finalSpeedIfCollide(List<Lane> blocks, Car myCar) {
        // Mengembalikan perhitungan kecepatan akhir jika melewati suatu lane
        int damage = 0;
        int speed_reduction = 0;
        int final_speed = -1;

        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (block.isOccupiedByCyberTruck) {
                return 0;
            }
            if (terrain == Terrain.WALL) {
                damage += 2;
            }
            else if (terrain == Terrain.MUD || terrain == Terrain.OIL_SPILL) {
                damage += 1;
            }
        }

        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (terrain == Terrain.WALL) {
                final_speed = 3;
            }
            else if ((terrain == Terrain.MUD || terrain == Terrain.OIL_SPILL) && final_speed == -1) {
                speed_reduction += 1;
            }
        }

        if (final_speed == -1) {
            final_speed = myCar.speed;
            for (int i = 0; i < speed_reduction; i++) {
                final_speed = speedIfDecelerate(myCar.speed);
            }
            final_speed = max(3, final_speed);
        }

        return min(final_speed, maxSpeed(myCar.damage + damage));
    }

    private float countPowerUpsPrioPoints(List<Lane> blocks) {
        // Mengembalikan jumlah powerup di dalam list blocks
        float powerUpsPrioPoints = 0;
        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            switch (terrain) {
                case BOOST:
                    powerUpsPrioPoints += 2;
                    break;
                case LIZARD:
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

}
