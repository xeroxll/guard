import { useState, useEffect, useRef } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Device from 'expo-device';
import * as Application from 'expo-application';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

export interface BlacklistedApp {
  id: string;
  name: string;
  packageName: string;
}

export interface LogEntry {
  id: string;
  type: 'block' | 'threat' | 'check';
  title: string;
  desc: string;
  time: string;
}

const STORAGE_KEYS = {
  PROTECTION_ENABLED: '@guardian/protection_enabled',
  BLACKLIST: '@guardian/blacklist',
  LOGS: '@guardian/logs',
  STATS: '@guardian/stats',
  USB_STATUS: '@guardian/usb_status',
};

// Configure notification behavior
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

export function useGuardian() {
  const [isProtectionEnabled, setIsProtectionEnabled] = useState(true);
  const [blacklist, setBlacklist] = useState<BlacklistedApp[]>([]);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [usbStatus, setUsbStatus] = useState(false);
  const [isScanning, setIsScanning] = useState(false);
  const [stats, setStats] = useState({
    threats: 0,
    blocks: 0,
    checks: 0,
  });

  const notificationListener = useRef<any>(null);
  const responseListener = useRef<any>(null);

  useEffect(() => {
    loadData();
    setupNotifications();
    
    // Simulate periodic checks
    const interval = setInterval(() => {
      if (isProtectionEnabled) {
        checkSecurityStatus();
      }
    }, 10000);

    notificationListener.current = Notifications.addNotificationReceivedListener(notification => {
      console.log('Notification received:', notification);
    });

    responseListener.current = Notifications.addNotificationResponseReceivedListener(response => {
      console.log('Notification response received:', response);
    });

    return () => {
      clearInterval(interval);
      if (notificationListener.current) notificationListener.current.remove();
      if (responseListener.current) responseListener.current.remove();
    };
  }, [isProtectionEnabled]);

  const setupNotifications = async () => {
    if (Platform.OS === 'web') return;
    
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;
    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }
    if (finalStatus !== 'granted') return;
  };

  const sendAlert = async (title: string, body: string) => {
    if (Platform.OS === 'web') {
      console.log(`[Notification] ${title}: ${body}`);
      return;
    }
    await Notifications.scheduleNotificationAsync({
      content: { title, body },
      trigger: null,
    });
  };

  const checkSecurityStatus = async () => {
    // In a real app, we'd use native modules here.
    // For now, we simulate detecting USB debugging or a blacklisted app attempt.
  };

  const loadData = async () => {
    try {
      const enabled = await AsyncStorage.getItem(STORAGE_KEYS.PROTECTION_ENABLED);
      if (enabled !== null) setIsProtectionEnabled(JSON.parse(enabled));

      const storedBlacklist = await AsyncStorage.getItem(STORAGE_KEYS.BLACKLIST);
      if (storedBlacklist) setBlacklist(JSON.parse(storedBlacklist));

      const storedLogs = await AsyncStorage.getItem(STORAGE_KEYS.LOGS);
      if (storedLogs) setLogs(JSON.parse(storedLogs));

      const storedStats = await AsyncStorage.getItem(STORAGE_KEYS.STATS);
      if (storedStats) setStats(JSON.parse(storedStats));

      const storedUsb = await AsyncStorage.getItem(STORAGE_KEYS.USB_STATUS);
      if (storedUsb) setUsbStatus(JSON.parse(storedUsb));
    } catch (e) {
      console.error('Failed to load data', e);
    }
  };

  const startScan = async () => {
    if (isScanning) return;
    setIsScanning(true);
    
    // Simulate scan delay
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const newStats = {
      ...stats,
      checks: stats.checks + 1,
      threats: usbStatus ? stats.threats + 1 : stats.threats,
      blocks: blacklist.length > 0 ? stats.blocks + 1 : stats.blocks,
    };
    
    setStats(newStats);
    await AsyncStorage.setItem(STORAGE_KEYS.STATS, JSON.stringify(newStats));
    
    addLog({
      type: 'check',
      title: 'Сканирование завершено',
      desc: usbStatus ? 'Обнаружены угрозы' : 'Устройство в безопасности',
    });
    
    setIsScanning(false);
  };

  const toggleProtection = async () => {
    const newValue = !isProtectionEnabled;
    setIsProtectionEnabled(newValue);
    await AsyncStorage.setItem(STORAGE_KEYS.PROTECTION_ENABLED, JSON.stringify(newValue));
    
    const statusMsg = newValue ? 'Защита включена' : 'Защита выключена';
    addLog({
      type: 'check',
      title: statusMsg,
      desc: newValue ? 'Активный мониторинг запущен' : 'Устройство в опасности',
    });

    if (!newValue) {
      sendAlert('Внимание!', 'Защита Guardian отключена. Ваше устройство уязвимо.');
    }
  };

  const addToBlacklist = async (app: Omit<BlacklistedApp, 'id'>) => {
    const newApp = { ...app, id: Date.now().toString() };
    const newBlacklist = [...blacklist, newApp];
    setBlacklist(newBlacklist);
    await AsyncStorage.setItem(STORAGE_KEYS.BLACKLIST, JSON.stringify(newBlacklist));
    
    addLog({
      type: 'block',
      title: 'Добавлено в чёрный список',
      desc: `${app.name} (${app.packageName})`,
    });
  };

  const removeFromBlacklist = async (id: string) => {
    const newBlacklist = blacklist.filter(a => a.id !== id);
    setBlacklist(newBlacklist);
    await AsyncStorage.setItem(STORAGE_KEYS.BLACKLIST, JSON.stringify(newBlacklist));
  };

  const addLog = async (entry: Omit<LogEntry, 'id' | 'time'>) => {
    const newEntry: LogEntry = {
      ...entry,
      id: Date.now().toString(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    const newLogs = [newEntry, ...logs].slice(0, 50);
    setLogs(newLogs);
    await AsyncStorage.setItem(STORAGE_KEYS.LOGS, JSON.stringify(newLogs));
  };

  const toggleUsbStatus = async () => {
    const newStatus = !usbStatus;
    setUsbStatus(newStatus);
    await AsyncStorage.setItem(STORAGE_KEYS.USB_STATUS, JSON.stringify(newStatus));
    
    if (newStatus) {
      addLog({
        type: 'threat',
        title: 'USB отладка включена',
        desc: 'Обнаружено потенциально опасное подключение',
      });
      sendAlert('Опасность!', 'Обнаружено включение USB-отладки. Это может быть угрозой.');
    }
  };

  return {
    isProtectionEnabled,
    toggleProtection,
    blacklist,
    addToBlacklist,
    removeFromBlacklist,
    logs,
    usbStatus,
    toggleUsbStatus,
    stats,
    startScan,
    isScanning,
  };
}
