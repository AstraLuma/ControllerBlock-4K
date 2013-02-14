-- -----------------------------------------------------
-- Table `ControllerBlock_Lord`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `ControllerBlock_Lord` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `world` VARCHAR(45) NOT NULL ,
  `x` INT(11) NOT NULL ,
  `y` INT(11) NOT NULL ,
  `z` INT(11) NOT NULL ,
  `owner` VARCHAR(45) NOT NULL ,
  `protection` ENUM('PROTECTED', 'SEMIPROTECTED', 'UNPROTECTED') NOT NULL DEFAULT 'PROTECTED' ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `LOCATION` (`world` ASC, `x` ASC, `y` ASC, `z` ASC) ,
  INDEX `OWNER` (`owner` ASC) ,
  INDEX `WORLD` (`world` ASC) )
ENGINE = InnoDB
COMMENT = 'Controlling blocks';


-- -----------------------------------------------------
-- Table `ControllerBlock_Serf`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `ControllerBlock_Serf` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `lord` INT UNSIGNED NOT NULL ,
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
    REFERENCES `ControllerBlock_Lord` (`id` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = 'Controlled blocks.\n\nTo add: Inventory, other data';

