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
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;

        List<Lane> blocksInFront = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Lane> blocksInFrontLeft = getBlocksSide(myCar.position.lane, myCar.position.block, gameState, LEFT);
        List<Lane> blocksInFrontRight = getBlocksSide(myCar.position.lane, myCar.position.block, gameState, RIGHT);
        List<Lane> blocksInBack = getBlocksBack(myCar.position.lane, myCar.position.block, gameState);
        List<Lane> blocksIfBoost = blocksInFront.subList(0, min(speedIfBoost(myCar.damage), blocksInFront.size() - 1));
        List<Lane> blocksIfAccelerate = blocksInFront.subList(0, min(speedIfAccelerate(myCar.speed, myCar.damage), blocksInFront.size() - 1));

        // GREEDY FIX
        // Fix jika damage lebih dari atau sama dengan 3 atau jika punya boost dan damage lebih dari atau sama dengan 1
        if (myCar.damage > 1) {
            return FIX;
        }

        if (myCar.speed == 0) {
            return ACCELERATE;
        }

        // GREEDY OBSTACLE AVOIDANCE
        // Hindari obstacle, cari jalur yang speed akhirnya paling besar, cari jalur yang dapat memberikan powerup terbanyak
        List<Lane> blocksIfNoAccelerate = blocksInFront.subList(0, myCar.speed);
        List<Lane> blocksIfLeft = emptyList();
        List<Lane> blocksIfRight = emptyList();
        if(isObstaclePresent(blocksIfNoAccelerate, opponent.id)) {
            if (myCar.position.lane != 1) {
                blocksIfLeft = blocksInFrontLeft.subList(0, min(myCar.speed, blocksInFrontLeft.size() - 1));
            }

            if (myCar.position.lane != 4) {
                blocksIfRight = blocksInFrontRight.subList(0, min(myCar.speed, blocksInFrontRight.size() - 1));
            }

            // Belok kiri jika tidak ada obstacle yang menghalangi di kiri, ada obstacle di kanan dan mobil tidak di lane 1
            if (!isObstaclePresent(blocksIfLeft, opponent.id) && isObstaclePresent(blocksIfRight, opponent.id) && myCar.position.lane != 1) {
                return TURN_LEFT;
            }
            // Belok kanan jika tidak ada obstacle yang menghalangi di kanan, ada obstacle di kiri, dan mobil tidak di lane 4
            if (!isObstaclePresent(blocksIfRight, opponent.id) && isObstaclePresent(blocksIfLeft, opponent.id) && myCar.position.lane != 4) {
                return TURN_RIGHT;
            }

            // Jika kedua lane tidak ada obstacle, ke lane yang power up nya lebih banyak
            if (!isObstaclePresent(blocksIfRight, opponent.id) && !isObstaclePresent(blocksIfLeft, opponent.id)) {
                if (myCar.position.lane == 1) {
                    return TURN_RIGHT;
                }
                if (myCar.position.lane == 4) {
                    return TURN_LEFT;
                }
                if (countPowerUps(blocksIfLeft) > countPowerUps(blocksIfRight)) {
                    return TURN_LEFT;
                }
                return TURN_RIGHT;
            }

            // Lizard jika kedua lane ada obstacle, punya lizard, dan tidak ada obstacle di titik akhir gerakan
            if (isObstaclePresent(blocksIfLeft, opponent.id) && isObstaclePresent(blocksIfRight, opponent.id) && hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }

            if (isObstaclePresent(blocksIfLeft, opponent.id) && isObstaclePresent(blocksIfRight, opponent.id)) {
                // Jika kedua lane ada obstacle, cari yang obstaclenya memiliki efek negatif lebih kecil
                // Jika kedua lane ada obstacle, efek negatifnya sama, cari yang ada powerupnya

                int leftFinalSpeed = finalSpeedIfCollide(blocksIfLeft, myCar, false);
                int centerFinalSpeed = finalSpeedIfCollide(blocksIfNoAccelerate, myCar, true);
                int rightFinalSpeed = finalSpeedIfCollide(blocksIfRight, myCar, false);

                int numOfPowerUpsLeft = countPowerUps(blocksIfLeft);
                int numOfPowerUpsCenter = countPowerUps(blocksIfAccelerate);
                int numOfPowerUpsRight = countPowerUps(blocksIfRight);

                if (myCar.position.lane == 1) {
                    if (centerFinalSpeed > rightFinalSpeed) {
                        return ACCELERATE;
                    }

                    if (centerFinalSpeed < rightFinalSpeed) {
                        return TURN_RIGHT;
                    }

                    if (numOfPowerUpsCenter > numOfPowerUpsRight) {
                        return ACCELERATE;
                    }

                    if (numOfPowerUpsCenter < numOfPowerUpsRight) {
                        return TURN_RIGHT;
                    }
                }
                else if (myCar.position.lane == 4) {
                    if (centerFinalSpeed > leftFinalSpeed) {
                        return ACCELERATE;
                    }
                    if (centerFinalSpeed < leftFinalSpeed) {
                        return TURN_LEFT;
                    }

                    if (numOfPowerUpsCenter > numOfPowerUpsLeft) {
                        return ACCELERATE;
                    }

                    if (numOfPowerUpsCenter < numOfPowerUpsLeft) {
                        return TURN_LEFT;
                    }
                }
                else {
                    if (leftFinalSpeed > centerFinalSpeed && leftFinalSpeed > rightFinalSpeed) {
                        return TURN_LEFT;
                    }

                    if (centerFinalSpeed >= leftFinalSpeed && centerFinalSpeed >= rightFinalSpeed) {
                        return ACCELERATE;
                    }

                    if (rightFinalSpeed > centerFinalSpeed && rightFinalSpeed > leftFinalSpeed) {
                        return TURN_RIGHT;
                    }


                    if (numOfPowerUpsLeft > numOfPowerUpsCenter && numOfPowerUpsLeft > numOfPowerUpsRight) {
                        return TURN_LEFT;
                    }

                    if (numOfPowerUpsCenter > numOfPowerUpsLeft && numOfPowerUpsCenter > numOfPowerUpsRight) {
                        return ACCELERATE;
                    }

                    if (numOfPowerUpsRight > numOfPowerUpsLeft && numOfPowerUpsRight > numOfPowerUpsCenter) {
                        return TURN_RIGHT;
                    }
                }
            }
        }

        // GREEDY OBSTACLE PLACEMENT
        else {
            // Gunkaan emp jika lawan di depan dan lawan ada di lane yang ada di range emp
            if (hasPowerUp(PowerUps.EMP, myCar.powerups) && isInEmpRange(myCar.position, opponent.position)) {
                return EMP;
            }

            // Gunakan tweet di depan lawan jika punya tweet
            if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
                return new TweetCommand(opponent.position.lane, opponent.position.block + speedIfAccelerate(opponent.speed, opponent.damage) + 1);
            }

            // Gunakan oil jika lawan ada di belakang dan di belakang tidak ada obstacle
            if (hasPowerUp(PowerUps.OIL, myCar.powerups) && myCar.position.block > opponent.position.block && (!isObstaclePresentWithoutOpponent(blocksInBack) || myCar.position.lane == opponent.position.lane)) {
                return OIL;
            }
        }

        // GREEDY BOOST
        // Boost jika speed akan bertambah saat command diberikan, tidak ada obstacle yang menghalangi, dan boost tersedia
        if (myCar.speed < speedIfBoost(myCar.damage) && !isObstaclePresent(blocksIfBoost, opponent.id) && hasPowerUp(PowerUps.BOOST, myCar.powerups) && !myCar.boosting) {
            return BOOST;
        }

        // GREEDY ACCELERATE
        // Accelerate jika speed akan bertambah saat command diberikan dan tidak ada obstacle yang menghalangi atau speed <= 3
        if ((myCar.speed < speedIfAccelerate(myCar.speed, myCar.damage) && !isObstaclePresent(blocksIfAccelerate, opponent.id)) || myCar.speed <= 3) {
            return ACCELERATE;
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
                final_speed = 6;
                break;
            case 5:
                final_speed = 6;
                break;
            case 6:
                final_speed = 8;
                break;
            case 8:
                final_speed = 9;
                break;
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
            case 5: return 3;
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

    private Boolean isObstaclePresent(List<Lane> blocks, int opponentID) {
        // Mengembalikan true jika ada obstacle di dalam list blocks
        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (terrain == Terrain.WALL || terrain == Terrain.MUD || terrain == Terrain.OIL_SPILL || block.isOccupiedByCyberTruck || block.occupiedByPlayerId == opponentID) {
                return true;
            }
        }
        return false;
    }

    private Boolean isObstaclePresentWithoutOpponent(List<Lane> blocks) {
        // Mengembalikan true jika ada obstacle di dalam list blocks (tidak termasuk musuh)
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

        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (terrain == Terrain.WALL || block.isOccupiedByCyberTruck) {
                damage += 2;
            }
            else if (terrain == Terrain.MUD || terrain == Terrain.OIL_SPILL) {
                damage += 1;
            }
        }

        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (block.isOccupiedByCyberTruck) {
                final_speed = 0;
                break;
            }
            else if (terrain == Terrain.WALL) {
                final_speed = 3;
            }
            else if ((terrain == Terrain.MUD || terrain == Terrain.OIL_SPILL) && final_speed == -1) {
                speed_reduction += 1;
            }
        }

        if (final_speed == -1) {
            if (isAccelerating) {
                final_speed = speedIfAccelerate(myCar.speed, myCar.damage);
            }
            else {
                final_speed = myCar.speed;
            }
            for (int i = 0; i < speed_reduction; i++) {
                final_speed = speedIfDecelerate(myCar.speed);
            }
        }

        return min(final_speed, maxSpeed(myCar.damage + damage));
    }

    private int countPowerUps(List<Lane> blocks) {
        // Mengembalikan jumlah powerup di dalam list blocks
        int numOfPowerUps = 0;
        for (Lane block: blocks) {
            Terrain terrain = block.terrain;
            if (terrain == Terrain.BOOST || terrain == Terrain.LIZARD || terrain == Terrain.EMP || terrain == Terrain.OIL_POWER || terrain == Terrain.TWEET) {
                numOfPowerUps++;
            }
        }
        return numOfPowerUps;
    }

}
