import React from 'react';
import { View, Text, SafeAreaView, ScrollView, Switch, Pressable, Alert } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useGuardian } from '@/context/GuardianContext';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function SettingsScreen() {
  const { isProtectionEnabled, toggleProtection, usbStatus, toggleUsbStatus } = useGuardian();

  const handleResetStats = () => {
    Alert.alert(
      'Сброс статистики',
      'Вы уверены, что хотите сбросить все показатели?',
      [
        { text: 'Отмена', style: 'cancel' },
        { 
          text: 'Сбросить', 
          style: 'destructive',
          onPress: async () => {
            await AsyncStorage.removeItem('@guardian/stats');
            Alert.alert('Готово', 'Статистика сброшена. Перезапустите приложение для обновления.');
          }
        }
      ]
    );
  };

  return (
    <SafeAreaView className="flex-1 bg-[#0a0b10]">
      <ScrollView className="flex-1 px-6 py-4">
        <Text className="text-2xl font-bold text-white">Настройки</Text>
        <Text className="mb-8 text-sm text-gray-400">Конфигурация параметров защиты</Text>

        <View className="rounded-3xl bg-[#12141c] p-6 mb-6">
          <View className="flex-row items-center justify-between">
            <View className="flex-row items-center">
              <View className="mr-4 h-10 w-10 items-center justify-center rounded-xl bg-[#1a2a1a]">
                <MaterialCommunityIcons name="shield-check" size={20} color="#22c55e" />
              </View>
              <View>
                <Text className="text-base font-bold text-white">Общая защита</Text>
                <Text className="text-xs text-gray-500">Активный мониторинг</Text>
              </View>
            </View>
            <Switch 
              value={isProtectionEnabled} 
              onValueChange={toggleProtection}
              trackColor={{ false: '#1a1c26', true: '#6366f1' }}
              thumbColor="white"
            />
          </View>
        </View>

        <View className="rounded-3xl bg-[#12141c] p-6 mb-6">
          <View className="flex-row items-center justify-between mb-6">
            <View className="flex-row items-center">
              <View className="mr-4 h-10 w-10 items-center justify-center rounded-xl bg-[#1a1a2a]">
                <MaterialCommunityIcons name="usb" size={20} color="#6366f1" />
              </View>
              <View>
                <Text className="text-base font-bold text-white">USB отладка</Text>
                <Text className="text-xs text-gray-500">Статус: {usbStatus ? 'Включена' : 'Выключена'}</Text>
              </View>
            </View>
            <Switch 
              value={usbStatus} 
              onValueChange={toggleUsbStatus}
              trackColor={{ false: '#1a1c26', true: '#ef4444' }}
              thumbColor="white"
            />
          </View>

          <View className="h-[1px] bg-gray-800 mb-6" />

          <Pressable className="flex-row items-center justify-between">
            <View className="flex-row items-center">
              <View className="mr-4 h-10 w-10 items-center justify-center rounded-xl bg-[#1a1c26]">
                <Ionicons name="accessibility" size={20} color="#94a3b8" />
              </View>
              <View>
                <Text className="text-base font-bold text-white">Спец. возможности</Text>
                <Text className="text-xs text-gray-500">Проверить статус службы</Text>
              </View>
            </View>
            <Ionicons name="chevron-forward" size={20} color="#4b5563" />
          </Pressable>
        </View>

        <View className="rounded-3xl bg-[#12141c] p-6 mt-6">
          <Pressable 
            onPress={handleResetStats}
            className="flex-row items-center justify-between"
          >
            <View className="flex-row items-center">
              <View className="mr-4 h-10 w-10 items-center justify-center rounded-xl bg-[#2a1a1a]">
                <MaterialCommunityIcons name="refresh" size={20} color="#ef4444" />
              </View>
              <Text className="text-base font-bold text-white">Сбросить статистику</Text>
            </View>
          </Pressable>
        </View>

        <View className="items-center py-10">
          <Text className="text-gray-600 text-[10px] uppercase font-bold tracking-widest">Guard v1.0.2</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
