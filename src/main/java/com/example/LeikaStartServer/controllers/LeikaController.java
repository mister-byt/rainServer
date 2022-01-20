package com.example.LeikaStartServer.controllers;

import com.example.LeikaStartServer.datebase.AccountRepository;
import com.example.LeikaStartServer.datebase.LeikaRepository;
import com.example.LeikaStartServer.objects.LeikaObject;
import com.example.LeikaStartServer.objects.ZoneObject;
import com.example.LeikaStartServer.service.AccountService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/leika")
@RequiredArgsConstructor
@Slf4j
public class LeikaController {

    private final AccountService accountService;

    private final AccountRepository accountRepository;
    private final LeikaRepository leikaRepository;

    private final PasswordEncoder passwordEncoder;

    @PostMapping(value = "/add_zone", consumes = "application/json", produces = "application/json") //добавить зону
    public LeikaObject addZone(Authentication authentication, Principal principal, @RequestBody  ZoneMapClass zone) throws JsonProcessingException {
        Optional<LeikaObject> leikaOptional = leikaRepository.findLeikaObjectByLeikaId(zone.leikaId);
        log.info("Попытка добавить зону к {}", zone.leikaId);
        if(leikaOptional.isPresent()){
            LeikaObject leika = leikaOptional.get();
            String accountId = leika.getAccountId();
            if(accountId.equals(authentication.getName())){
                ArrayList<ZoneObject> zones = leika.getZones();

                /*ObjectMapper mapper = new ObjectMapper();
                ZoneObject zone = mapper.readValue(body.get("zone"), ZoneObject.class);*/
                ZoneObject zoneIncoming = zone.zone;
                zones.add(zoneIncoming);

                zones.sort(Comparator.comparing(ZoneObject::getZoneNum));
                leika.setZones(zones);
                leikaRepository.save(leika);
                return leika;
                //throw new ResponseStatusException(HttpStatus.CREATED, "Зона успешно добавлена");
            } else {
                log.error("Лейка привязана к другому аккаунту. Доступ запрещен");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Лейка привязана к другому аккаунту. Доступ запрещен");
            }
        } else {
            log.error("Лейки с id {} не существует", zone.leikaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лейки с id " + zone.leikaId + "не существует");
        }
    }

    static class ZoneMapClass{

        String leikaId;
        ZoneObject zone;

        public ZoneMapClass(String leikaId, ZoneObject zone) {
            this.leikaId = leikaId;
            this.zone = zone;
        }
    }

    @PostMapping(value = "/on_zone") //добавить зону
    public LeikaObject onZone(Authentication authentication, @RequestParam String leikaId, int zoneNumInList, int armTime) {
        Optional<LeikaObject> leikaOptional = leikaRepository.findLeikaObjectByLeikaId(leikaId);
        log.info("Попытка включить зону к {}", leikaId);
        if (leikaOptional.isPresent()) {
            LeikaObject leika = leikaOptional.get();
            String accountId = leika.getAccountId();
            if (accountId.equals(authentication.getName())) {
                ArrayList<ZoneObject> zones = leika.getZones();

                for (int i = 0; i < zones.size(); i++) {
                    if (i == zoneNumInList) {
                        zones.get(i).setOnOff(true);
                        zones.get(i).setArmTime(armTime);
                    } else {
                        zones.get(i).setOnOff(false);
                    }
                }
                leika.setZones(zones);
                leika.setWateringType(1);
                leikaRepository.save(leika);
                return leika;
                //throw new ResponseStatusException(HttpStatus.CREATED, "Зона включена");
            } else {
                log.error("Лейка привязана к другому аккаунту. Доступ запрещен");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Лейка привязана к другому аккаунту. Доступ запрещен");
            }
        } else {
            log.error("Лейки с id {} не существует", leikaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лейки с id " + leikaId + "не существует");
        }
    }

    @PostMapping(value = "/off_zone") //добавить зону
    public LeikaObject offZone(Authentication authentication, @RequestParam String leikaId, int zoneNumInList) {
        Optional<LeikaObject> leikaOptional = leikaRepository.findLeikaObjectByLeikaId(leikaId);
        log.info("Попытка выключить зону к {}", leikaId);
        if (leikaOptional.isPresent()) {
            LeikaObject leika = leikaOptional.get();
            String accountId = leika.getAccountId();
            if (accountId.equals(authentication.getName())) {
                ArrayList<ZoneObject> zones = leika.getZones();
                zones.get(zoneNumInList).setOnOff(false);
                leika.setZones(zones);
                leika.setWateringType(0);
                leikaRepository.save(leika);
                return leika;
                //throw new ResponseStatusException(HttpStatus.CREATED, "Зона выключена");
            } else {
                log.error("Лейка привязана к другому аккаунту. Доступ запрещен");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Лейка привязана к другому аккаунту. Доступ запрещен");
            }
        } else {
            log.error("Лейки с id {} не существует", leikaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лейки с id " + leikaId + "не существует");
        }
    }

    @PostMapping(value = "/rename_zone") //добавить зону
    public LeikaObject renameZone(Authentication authentication, @RequestParam String leikaId, int zoneNumInList, String zoneName) {
        Optional<LeikaObject> leikaOptional = leikaRepository.findLeikaObjectByLeikaId(leikaId);
        log.info("Попытка переименовать зону к {}", leikaId);
        if (leikaOptional.isPresent()) {
            LeikaObject leika = leikaOptional.get();
            String accountId = leika.getAccountId();
            if (accountId.equals(authentication.getName())) {
                ArrayList<ZoneObject> zones = leika.getZones();
                zones.get(zoneNumInList).setName(zoneName);
                leika.setZones(zones);
                leikaRepository.save(leika);
                return leika;
                //throw new ResponseStatusException(HttpStatus.CREATED, "Зона переименована");
            } else {
                log.error("Лейка привязана к другому аккаунту. Доступ запрещен");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Лейка привязана к другому аккаунту. Доступ запрещен");
            }
        } else {
            log.error("Лейки с id {} не существует", leikaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лейки с id " + leikaId + "не существует");
        }
    }

    @PostMapping(value = "/delete_zone") //добавить зону
    public LeikaObject deleteZone(Authentication authentication, @RequestParam String leikaId, int zoneNumInList) {
        Optional<LeikaObject> leikaOptional = leikaRepository.findLeikaObjectByLeikaId(leikaId);
        log.info("Попытка удалить зону {} Лейки {} ", leikaId, zoneNumInList);
        if (leikaOptional.isPresent()) {
            LeikaObject leika = leikaOptional.get();
            String accountId = leika.getAccountId();
            if (accountId.equals(authentication.getName())) {
                ArrayList<ZoneObject> zones = leika.getZones();
                zones.remove(zoneNumInList);
                leika.setZones(zones);
                leikaRepository.save(leika);
                return leika;
                //throw new ResponseStatusException(HttpStatus.CREATED, "Зона удалена");
            } else {
                log.error("Лейка привязана к другому аккаунту. Доступ запрещен");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Лейка привязана к другому аккаунту. Доступ запрещен");
            }
        } else {
            log.error("Лейки с id {} не существует", leikaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лейки с id " + leikaId + "не существует");
        }
    }

    @PostMapping(value = "/change_icon") //добавить зону
    public LeikaObject changeIcon(Authentication authentication, @RequestParam String leikaId, int zoneNumInList, int zoneIcon) {
        Optional<LeikaObject> leikaOptional = leikaRepository.findLeikaObjectByLeikaId(leikaId);
        log.info("Попытка изменить иконку Лейки {} у зоны {}", leikaId, zoneNumInList);
        if (leikaOptional.isPresent()) {
            LeikaObject leika = leikaOptional.get();
            String accountId = leika.getAccountId();
            if (accountId.equals(authentication.getName())) {
                ArrayList<ZoneObject> zones = leika.getZones();
                zones.get(zoneNumInList).setIcon(zoneIcon);
                leika.setZones(zones);
                leikaRepository.save(leika);
                return leika;
                //throw new ResponseStatusException(HttpStatus.CREATED, "Иконка изменена");
            } else {
                log.error("Лейка привязана к другому аккаунту. Доступ запрещен");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Лейка привязана к другому аккаунту. Доступ запрещен");
            }
        } else {
            log.error("Лейки с id {} не существует", leikaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лейки с id " + leikaId + "не существует");
        }
    }

    @PostMapping(value = "/change_sch_time") //добавить зону
    public LeikaObject changeSchTime(Authentication authentication, @RequestParam String leikaId, int zoneNumInList, int schTime) {
        Optional<LeikaObject> leikaOptional = leikaRepository.findLeikaObjectByLeikaId(leikaId);
        log.info("Попытка изменить иконку Лейки {} у зоны {}", leikaId, zoneNumInList);
        if (leikaOptional.isPresent()) {
            LeikaObject leika = leikaOptional.get();
            String accountId = leika.getAccountId();
            if (accountId.equals(authentication.getName())) {
                ArrayList<ZoneObject> zones = leika.getZones();
                zones.get(zoneNumInList).setSchTime(schTime);
                leika.setZones(zones);
                leikaRepository.save(leika);
                return leika;
                //throw new ResponseStatusException(HttpStatus.CREATED, "Иконка изменена");
            } else {
                log.error("Лейка привязана к другому аккаунту. Доступ запрещен");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Лейка привязана к другому аккаунту. Доступ запрещен");
            }
        } else {
            log.error("Лейки с id {} не существует", leikaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лейки с id " + leikaId + "не существует");
        }
    }
}
