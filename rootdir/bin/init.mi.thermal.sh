# Force Fast Charging
if [ -e /sys/kernel/fast_charge/force_fast_charge ]; then
    chmod 0664 /sys/kernel/fast_charge/force_fast_charge
    echo 1 > /sys/kernel/fast_charge/force_fast_charge
fi

# Battery Input Suspend
if [ -e /sys/class/power_supply/battery/battery_input_suspend ]; then
    chmod 0664 /sys/class/power_supply/battery/battery_input_suspend
    echo 1 > /sys/class/power_supply/battery/battery_input_suspend
fi

# Delay before thermal change
sleep 20

# Thermal Evaluation Override
if [ -e /sys/class/thermal/thermal_message/sconfig ]; then
    chmod 0664 /sys/class/thermal/thermal_message/sconfig
    echo 10 > /sys/class/thermal/thermal_message/sconfig
fi
