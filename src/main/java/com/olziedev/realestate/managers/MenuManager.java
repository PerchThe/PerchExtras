package com.olziedev.realestate.managers;

import com.olziedev.olziemenu.OlzieMenu;
import com.olziedev.realestate.RealEstate;
import com.olziedev.realestate.menus.Menu;
import com.olziedev.realestate.menus.guis.*;

import java.util.ArrayList;
import java.util.List;

public class MenuManager extends Manager {

    private final List<Menu> menus;
    private final OlzieMenu olzieMenu;

    public MenuManager(RealEstate plugin) {
        super(plugin);
        menus = new ArrayList<>();
        this.olzieMenu = new OlzieMenu(plugin.plugin);
    }

    @Override
    public void setup() {
        menus.add(new BuyEstateMenu(this));
        menus.add(new RentEstateMenu(this));
        menus.add(new RentManageMenu(this));
        menus.add(new RentCycleMenu(this));
        menus.add(new RentOwnerMenu(this));
        menus.add(new RentAdminMenu(this));
        menus.add(new RentRenewMenu(this));
        menus.add(new RentHoldMenu(this));
        menus.add(new RentHoldOwnerMenu(this));
    }

    @Override
    public void load() {
        menus.forEach(Menu::load);
    }

    @SuppressWarnings("unchecked")
    public <T extends Menu> T getMenu(Class<T> clazz) {
        return menus.stream().filter(x -> x.getClass().equals(clazz)).map(x -> (T) x).findFirst().orElse(null);
    }

    public OlzieMenu getOlzieMenu() {
        return this.olzieMenu;
    }
}
