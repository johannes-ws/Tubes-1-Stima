package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import java.security.SecureRandom;

import static java.lang.Math.*;

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

        // Fix jika damage lebih dari atau sama dengan 4 atau jika punya boost dan damage lebih dari atau sama dengan 2
        if(myCar.damage >= 3 || (myCar.damage >= 2 && hasPowerUp(PowerUps.BOOST, myCar.powerups))) {
            return FIX;
        }

        // Boost jika speed akan bertambah saat command diberikan, tidak ada obstacle yang menghalangi, dan boost tersedia
        List<Object> blocksIfBoost = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState, speedIfBoost(myCar.damage));
        if(myCar.speed < speedIfBoost(myCar.damage) && !(isObstaclePresent(blocksIfBoost)) && hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
            return BOOST;
        }

        // Accelerate jika speed akan bertambah saat command diberikan dan tidak ada obstacle yang menghalangi
        List<Object> blocksIfAccelerate = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState, speedIfAccelerate(myCar.speed, myCar.damage));
        if((myCar.speed < speedIfAccelerate(myCar.speed, myCar.damage) && !(isObstaclePresent(blocksIfAccelerate))) || myCar.speed <= 6) {
            return ACCELERATE;
        }


        List<Object> blocksIfNoAccelerate = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState, myCar.speed);
        // Hindari obstacle atau cari obstacle yang efek negatifnya lebih kecil
        if(isObstaclePresent(blocksIfNoAccelerate)) {
            List<Object> blocksIfLeft = getBlocksSide(myCar.position.lane, myCar.position.block, gameState, LEFT, myCar.speed - 1);
            List<Object> blocksIfRight = getBlocksSide(myCar.position.lane, myCar.position.block, gameState, RIGHT, myCar.speed - 1);
            // Belok kiri jika tidak ada obstacle yang menghalangi di kiri, ada obstacle di kanan dan mobil tidak di lane 1
            if(!isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight) && myCar.position.lane != 1) {
                return TURN_LEFT;
            }
            // Belok kanan jika tidak ada obstacle yang menghalangi di kanan, ada obstacle di kiri, dan mobil tidak di lane 4
            if(!isObstaclePresent(blocksIfRight) && isObstaclePresent(blocksIfLeft) && myCar.position.lane != 4) {
                return TURN_RIGHT;
            }
            // Jika kedua lane tidak ada obstacle, ke lane yang power up nya lebih banyak
            if(!isObstaclePresent(blocksIfRight) && !isObstaclePresent(blocksIfLeft)) {
                if (myCar.position.lane == 1) {
                    return TURN_RIGHT;
                }
                if (myCar.position.lane == 4) {
                    return TURN_LEFT;
                }
                // SEMENTARA
                // SEMENTARA
                // SEMENTARA
                return TURN_LEFT;

            }
            // Lizard jika kedua lane ada obstacle, punya lizard, dan tidak ada obstacle di titik akhir gerakan
            if(isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight) && hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            // Decelerate jika obstacle bisa dihindari dengan command ini
            List<Object> blocksIfDecelerate = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState, speedIfDecelerate(myCar.speed));
            if(!isObstaclePresent(blocksIfDecelerate)) {
                return DECELERATE;
            }
            // SEMENTARA
            // SEMENTARA
            // SEMENTARA
            if (isObstaclePresent(blocksIfLeft) && isObstaclePresent(blocksIfRight)) {
                return ACCELERATE;
            }
            // Jika kedua lane ada obstacle, cari yang obstaclenya memiliki efek negatif lebih kecil

            // Jika kedua lane ada obstacle, efek negatifnya sama, cari yang ada powerupnya
        }
        // Tidak ada obstacle
        else {
            // Gunakan tweet di depan lawan jika punya tweet dan lawan bisa terlihat
            List<Object> blocksInFrontOfOpponent = getBlocksInFront(opponent.position.lane, opponent.position.block, gameState, opponent.speed);
            if (hasPowerUp(PowerUps.TWEET, myCar.powerups) && !isObstaclePresent(blocksInFrontOfOpponent)) {
                return new TweetCommand(opponent.position.lane, opponent.position.block + 2);
            }

            // Gunakan oil jika lawan ada di belakang dan di belakang tidak ada obstacle
            List<Object> blocksFromStart = getBlocksFromStart(myCar.position.lane, myCar.position.block, gameState, 5);
            if (hasPowerUp(PowerUps.OIL, myCar.powerups) && myCar.position.block > opponent.position.block && !isObstaclePresent(blocksFromStart)) {
                return OIL;
            }
            // Gunkaan emp jika lawan di depan dan lawan ada di lane yang ada di range emp
            if (hasPowerUp(PowerUps.EMP, myCar.powerups) && isInEmpRange(myCar.position, opponent.position)) {
                return EMP;
            }
        }

        return NOTHING;
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }


    private List<Object> getBlocksInFront(int lane, int block, GameState gameState, int numOfBlocks) {
        // Mengembalikan block sebanyak numOfBlocks yang ada di depan posisi (lane dan block)
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= min(block - startBlock + numOfBlocks, 25); i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private List<Object> getBlocksSide(int lane, int block, GameState gameState, int direction, int numOfBlocks) {
        // Mengembalikan block sebanyak numOfBlocks yang ada di lane samping posisi (lane dan block)
        if (lane + direction > 4 || lane + direction < 1) {
            return Collections.emptyList();
        }
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock - abs(direction), 0); i <= block - startBlock - abs(direction)+ numOfBlocks; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private List<Object> getBlocksFromStart(int lane, int block, GameState gameState, int numOfBlocks) {
        // Mengembalikan block sebanyak numOfBlocks yang ada di suatu lane
        // dimulai dari block paling belakang yang terlihat
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock - 5, 0); i <= block - startBlock + numOfBlocks; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

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

        int max_speed;
        switch (damage) {
            case 4:
                max_speed = 3;
                break;
            case 3:
                max_speed = 6;
                break;
            case 2:
                max_speed = 8;
                break;
            case 1:
                max_speed = 9;
                break;
            case 0:
                max_speed = 15;
                break;
            default: max_speed = 0;
        }

        if (final_speed > max_speed) {
            final_speed = max_speed;
        }

        return  final_speed;
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
        if (myCarPosition.block + 8 < opponentPosition.block) {
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

    private Boolean isObstaclePresent(List<Object> blocks) {
        // Mengembalikan true jika ada obstacle di suatu lane, pengecekan dimulai dari block paling belakang
        // yang terlihat hingga sebanyak numOfBlocks
        return blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || blocks.contains(Terrain.WALL);
    }

    /*
    private int obstacleWeight(List<Object> blocks) {

    }

    private int countPowerUps(List<Object> blocks) {

    }
    */
}
