SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `tekkit` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;

USE `tekkit`;

CREATE  TABLE IF NOT EXISTS `tekkit`.`ControllerBlock_Lord` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  `world` VARCHAR(45) NOT NULL ,
  `x` INT(11) NOT NULL ,
  `y` INT(11) NOT NULL ,
  `z` INT(11) NOT NULL ,
  `owner` VARCHAR(45) NOT NULL ,
  `protection` ENUM('PROTECTED', 'SEMIPROTECTED', 'UNPROTECTED') NOT NULL DEFAULT 'PROTECTED' ,
  PRIMARY KEY (`id`) ,
  INDEX `LOCATION` (`x` ASC, `y` ASC, `z` ASC, `world` ASC) ,
  INDEX `OWNER` (`owner` ASC) ,
  INDEX `WORLD` (`world` ASC) )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci
COMMENT = 'Controlling blocks';

CREATE  TABLE IF NOT EXISTS `tekkit`.`ControllerBlock_Serf` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT ,
  `lord` INT(10) UNSIGNED NOT NULL ,
  `world` VARCHAR(45) NOT NULL ,
  `x` INT(11) NOT NULL ,
  `y` INT(11) NOT NULL ,
  `z` INT(11) NOT NULL ,
  `material` INT(11) NOT NULL ,
  `meta` TINYINT(4) NOT NULL DEFAULT 0 ,
  PRIMARY KEY (`id`) ,
  INDEX `PARENT` (`lord` ASC) ,
  INDEX `LOCATION` (`world` ASC, `x` ASC, `y` ASC, `z` ASC) ,
  CONSTRAINT `PARENT`
    FOREIGN KEY (`lord` )
    REFERENCES `tekkit`.`ControllerBlock_Lord` (`id` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_general_ci
COMMENT = 'Controlled blocks';


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;