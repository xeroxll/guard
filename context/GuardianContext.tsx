import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import * as IntentLauncher from 'expo-intent-launcher';
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

interface GuardianContextType {
  isProtectionEnabled: boolean;
  toggleProtection: () => Promise<void>;
  blacklist: BlacklistedApp[];
  addToBlacklist: (app: Omit<BlacklistedApp, 'id'>) => Promise<void>;
  removeFromBlacklist: (id: string) => Promise<void>;
  logs: LogEntry[];
  usbStatus: boolean;
  toggleUsbStatus: () => Promise<void>;
  stats: {
    threats: number;
    blocks: number;
    checks: number;
  };
  startScan: () => Promise<void>;
  isScanning: boolean;
  isInitialized: boolean;
  openSettings: (type: 'usb' | 'apps' | 'security') => Promise<void>;
}

const STORAGE_KEYS = {
  PROTECTION_ENABLED: '@guardian/protection_enabled',
  BLACKLIST: '@guardian/blacklist',
  LOGS: '@guardian/logs',
  STATS: '@guardian/stats',
  USB_STATUS: '@guardian/usb_status',
};

const GuardianContext = createContext<GuardianContextType | undefined>(undefined);

export function GuardianProvider({ children }: { children: React.ReactNode }) {
  const [isInitialized, setIsInitialized] = useState(false);
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
    const init = async () => {
      try {
        await loadData();
      } catch (e) {
        console.warn("Init failed:", e);
      } finally {
        setIsInitialized(true);
      }
    };
    init();

    if (Platform.OS !== 'web') {
      setupNotifications();
      notificationListener.current = Notifications.addNotificationReceivedListener(() => {});
      responseListener.current = Notifications.addNotificationResponseReceivedListener(() => {});
    }

    const interval = setInterval(() => {
      if (isProtectionEnabled) {
        // Background task simulation
      }
    }, 30000);

    return () => {
      clearInterval(interval);
      if (notificationListener.current) notificationListener.current.remove();
      if (responseListener.current) responseListener.current.remove();
    };
  }, []);

  const setupNotifications = async () => {
    try {
      const { status: existingStatus } = await Notifications.getPermissionsAsync();
      if (existingStatus !== 'granted') {
        await Notifications.requestPermissionsAsync();
      }
    } catch (e) {
      console.warn('Notifications setup failed', e);
    }
  };

  const loadData = async () => {
    try {
      const [enabled, storedBlacklist, storedLogs, storedStats, storedUsb] = await Promise.all([
        AsyncStorage.getItem(STORAGE_KEYS.PROTECTION_ENABLED),
        AsyncStorage.getItem(STORAGE_KEYS.BLACKLIST),
        AsyncStorage.getItem(STORAGE_KEYS.LOGS),
        AsyncStorage.getItem(STORAGE_KEYS.STATS),
        AsyncStorage.getItem(STORAGE_KEYS.USB_STATUS),
      ]);

      if (enabled !== null) setIsProtectionEnabled(JSON.parse(enabled));
      if (storedBlacklist) setBlacklist(JSON.parse(storedBlacklist));
      if (storedLogs) setLogs(JSON.parse(storedLogs));
      if (storedStats) setStats(JSON.parse(storedStats));
      if (storedUsb) setUsbStatus(JSON.parse(storedUsb));
    } catch (e) {
      console.error('Load data failed', e);
    }
  };

  const addLog = async (entry: Omit<LogEntry, 'id' | 'time'>) => {
    const newEntry: LogEntry = {
      ...entry,
      id: Date.now().toString(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    setLogs(prev => {
      const updated = [newEntry, ...prev].slice(0, 50);
      AsyncStorage.setItem(STORAGE_KEYS.LOGS, JSON.stringify(updated));
      return updated;
    });
  };

  const sendAlert = async (title: string, body: string) => {
    if (Platform.OS === 'web') return;
    try {
      await Notifications.scheduleNotificationAsync({
        content: { title, body },
        trigger: null,
      });
    } catch (e) {
      console.warn('Alert failed', e);
    }
  };

  const toggleProtection = async () => {
    const newValue = !isProtectionEnabled;
    setIsProtectionEnabled(newValue);
    await AsyncStorage.setItem(STORAGE_KEYS.PROTECTION_ENABLED, JSON.stringify(newValue));
    
    addLog({
      type: 'check',
      title: newValue ? 'Защита включена' : 'Защита выключена',
      desc: newValue ? 'Активный мониторинг запущен' : 'Устройство уязвимо',
    });

    if (!newValue) {
      await sendAlert('Внимание!', 'Защита Guardian отключена.');
    }
  };

  const addToBlacklist = async (app: Omit<BlacklistedApp, 'id'>) => {
    const newApp = { ...app, id: Date.now().toString() };
    const updated = [...blacklist, newApp];
    setBlacklist(updated);
    await AsyncStorage.setItem(STORAGE_KEYS.BLACKLIST, JSON.stringify(updated));
    addLog({ type: 'block', title: 'Приложение заблокировано', desc: app.name });
  };

  const removeFromBlacklist = async (id: string) => {
    const updated = blacklist.filter(a => a.id !== id);
    setBlacklist(updated);
    await AsyncStorage.setItem(STORAGE_KEYS.BLACKLIST, JSON.stringify(updated));
  };

  const toggleUsbStatus = async () => {
    const newStatus = !usbStatus;
    setUsbStatus(newStatus);
    await AsyncStorage.setItem(STORAGE_KEYS.USB_STATUS, JSON.stringify(newStatus));
    if (newStatus) {
      addLog({ type: 'threat', title: 'USB отладка включена', desc: 'Обнаружена угроза' });
      await sendAlert('Опасность!', 'USB-отладка включена.');
    }
  };

  const startScan = async () => {
    if (isScanning) return;
    setIsScanning(true);
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const newStats = {
      checks: stats.checks + 1,
      threats: usbStatus ? stats.threats + 1 : stats.threats,
      blocks: stats.blocks,
    };
    setStats(newStats);
    await AsyncStorage.setItem(STORAGE_KEYS.STATS, JSON.stringify(newStats));
    addLog({ type: 'check', title: 'Сканирование завершено', desc: 'Статус обновлен' });
    setIsScanning(false);
  };

  const openSettings = async (type: 'usb' | 'apps' | 'security') => {
    if (Platform.OS !== 'android') {
      alert('Эта функция доступна только на Android устройствах');
      return;
    }

    try {
      if (type === 'usb') {
        await IntentLauncher.startActivityAsync(IntentLauncher.ActivityAction.DEVELOPER_SETTINGS);
      } else if (type === 'apps') {
        await IntentLauncher.startActivityAsync(IntentLauncher.ActivityAction.APPLICATION_SETTINGS);
      } else if (type === 'security') {
        await IntentLauncher.startActivityAsync(IntentLauncher.ActivityAction.SECURITY_SETTINGS);
      }
    } catch (e) {
      console.error('Failed to open settings', e);
    }
  };

  return (
    <GuardianContext.Provider
      value={{
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
        isInitialized,
        openSettings,
      }}
    >
      {children}
    </GuardianContext.Provider>
  );
}

export const useGuardian = () => {
  const context = useContext(GuardianContext);
  if (!context) throw new Error('useGuardian must be used within GuardianProvider');
  return context;
};
