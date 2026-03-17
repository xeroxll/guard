import React from 'react';
import { View, Text, ScrollView, Pressable, SafeAreaView } from 'react-native';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { useGuardian } from '@/hooks/useGuardian';
import { useRouter } from 'expo-router';

export default function HomeScreen() {
  const { isProtectionEnabled, stats, usbStatus, toggleProtection, startScan, isScanning } = useGuardian();
  const router = useRouter();

  return (
    <SafeAreaView className="flex-1 bg-[#0a0b10]">
      <ScrollView className="flex-1" contentContainerStyle={{ paddingBottom: 100 }}>
        {/* Header */}
        <View className="flex-row items-center justify-between px-6 py-4">
          <View className="flex-row items-center">
            <View className="mr-3 rounded-xl bg-[#1a1c26] p-2">
              <MaterialCommunityIcons name="shield-check" size={28} color="white" />
            </View>
            <View>
              <Text className="text-2xl font-bold text-white leading-tight">Guardian</Text>
              <Text className="text-xs text-gray-400">Защита {isProtectionEnabled ? 'активна' : 'выключена'}</Text>
            </View>
          </View>
          <Pressable 
            className="rounded-xl bg-[#1a1c26] p-3"
            onPress={() => router.push('/settings')}
          >
            <Feather name="settings" size={20} color="white" />
          </Pressable>
        </View>

        {/* Status Badge */}
        <View className="px-6 py-2">
          <Pressable 
            onPress={toggleProtection}
            className={`w-44 flex-row items-center rounded-full px-3 py-1.5 ${isProtectionEnabled ? 'bg-[#1a2a1a]' : 'bg-[#2a1a1a]'}`}
          >
            <View className={`mr-2 h-2 w-2 rounded-full ${isProtectionEnabled ? 'bg-green-500' : 'bg-red-500'}`} />
            <Text className="text-xs font-medium text-white">
              Защита {isProtectionEnabled ? 'включена' : 'выключена'}
            </Text>
          </Pressable>
        </View>

        {/* Main Shield Icon */}
        <View className="my-10 items-center justify-center">
          <Pressable 
            onPress={startScan}
            disabled={isScanning}
            className={`h-44 w-44 items-center justify-center rounded-full shadow-2xl ${
              isScanning ? 'bg-[#1a1c26]' : isProtectionEnabled ? 'bg-[#161821]' : 'bg-[#1a1313]'
            }`}
          >
            <View className={`h-36 w-36 items-center justify-center rounded-full ${
              isScanning ? 'bg-[#252836]' : isProtectionEnabled ? 'bg-[#1e2130]' : 'bg-[#301e1e]'
            }`}>
              {isScanning ? (
                <View className="items-center">
                  <MaterialCommunityIcons name="loading" size={64} color="#6366f1" className="animate-spin" />
                  <Text className="mt-2 text-[10px] font-bold text-[#6366f1] uppercase">Сканирование...</Text>
                </View>
              ) : (
                <MaterialCommunityIcons 
                  name={isProtectionEnabled ? "shield-check" : "shield-off"} 
                  size={72} 
                  color="white" 
                />
              )}
            </View>
          </Pressable>
        </View>

        {/* Stats Row */}
        <View className="flex-row justify-between px-6">
          <View className="items-center flex-1">
            <View className="mb-2 rounded-lg bg-[#1a1c26] p-2">
              <MaterialCommunityIcons name="usb" size={20} color={usbStatus ? "#ef4444" : "#60a5fa"} />
            </View>
            <Text className="text-xs font-bold text-[#60a5fa]">USB</Text>
            <Text className="text-xs text-white">✓ {usbStatus ? 'Вкл' : 'Выкл'}</Text>
          </View>
          <View className="h-12 w-[1px] bg-gray-800 self-center" />
          <View className="items-center flex-1">
            <View className="mb-2 rounded-lg bg-[#1a1c26] p-2">
              <MaterialCommunityIcons name="cancel" size={20} color="#f472b6" />
            </View>
            <Text className="text-xs font-bold text-white">{stats.blocks}</Text>
            <Text className="text-xs text-gray-400">Блокировок</Text>
          </View>
          <View className="h-12 w-[1px] bg-gray-800 self-center" />
          <View className="items-center flex-1">
            <View className="mb-2 rounded-lg bg-[#1a1c26] p-2">
              <Feather name="clock" size={20} color="#fbbf24" />
            </View>
            <Text className="text-xs font-bold text-white">24ч</Text>
            <Text className="text-xs text-gray-400">Работает</Text>
          </View>
        </View>

        {/* Threat/Block/Check Cards */}
        <View className="mt-8 flex-row justify-between px-6 gap-3">
          <View className="flex-1 items-center rounded-3xl bg-[#12141c] p-4 py-6">
            <View className="mb-3 rounded-xl bg-[#2a1a1a] p-3">
              <MaterialCommunityIcons name="alert" size={24} color="#ef4444" />
            </View>
            <Text className="text-2xl font-bold text-white">{stats.threats}</Text>
            <Text className="text-xs text-gray-400">Угроз</Text>
          </View>
          <View className="flex-1 items-center rounded-3xl bg-[#12141c] p-4 py-6">
            <View className="mb-3 rounded-xl bg-[#1a2a1a] p-3">
              <MaterialCommunityIcons name="shield" size={24} color="#22c55e" />
            </View>
            <Text className="text-2xl font-bold text-white">{stats.blocks}</Text>
            <Text className="text-xs text-gray-400">Блокировок</Text>
          </View>
          <View className="flex-1 items-center rounded-3xl bg-[#12141c] p-4 py-6">
            <View className="mb-3 rounded-xl bg-[#1a1a2a] p-3">
              <MaterialCommunityIcons name="scan-helper" size={24} color="#6366f1" />
            </View>
            <Text className="text-2xl font-bold text-white">{stats.checks}</Text>
            <Text className="text-xs text-gray-400">Проверок</Text>
          </View>
        </View>

        {/* Management Section */}
        <View className="mt-10 px-6">
          <Text className="mb-4 text-xs font-bold uppercase tracking-widest text-gray-500">Управление</Text>
          <View className="flex-row flex-wrap justify-between gap-y-4">
            <Pressable 
              className="w-[48%] rounded-3xl bg-[#12141c] p-5"
              onPress={() => router.push('/blocking')}
            >
              <View className="mb-4 h-12 w-12 items-center justify-center rounded-2xl bg-[#2a1a1a]">
                <MaterialCommunityIcons name="block-helper" size={24} color="#ef4444" />
              </View>
              <Text className="text-base font-bold text-white">Чёрный список</Text>
              <Text className="text-[10px] text-gray-500">Управление</Text>
            </Pressable>
            <Pressable 
              className="w-[48%] rounded-3xl bg-[#12141c] p-5"
              onPress={() => router.push('/log')}
            >
              <View className="mb-4 h-12 w-12 items-center justify-center rounded-2xl bg-[#1a1a2a]">
                <Ionicons name="document-text" size={24} color="#6366f1" />
              </View>
              <Text className="text-base font-bold text-white">Журнал</Text>
              <Text className="text-[10px] text-gray-500">События</Text>
            </Pressable>
            <Pressable className="w-[48%] rounded-3xl bg-[#12141c] p-5">
              <View className="mb-4 h-12 w-12 items-center justify-center rounded-2xl bg-[#2a2a1a]">
                <MaterialCommunityIcons name="bell" size={24} color="#fbbf24" />
              </View>
              <Text className="text-base font-bold text-white">Уведомления</Text>
              <Text className="text-[10px] text-gray-500">Push-алерты</Text>
            </Pressable>
            <Pressable 
              className="w-[48%] rounded-3xl bg-[#12141c] p-5"
              onPress={() => router.push('/settings')}
            >
              <View className="mb-4 h-12 w-12 items-center justify-center rounded-2xl bg-[#1a1c26]">
                <Ionicons name="settings-sharp" size={24} color="#94a3b8" />
              </View>
              <Text className="text-base font-bold text-white">Настройки</Text>
              <Text className="text-[10px] text-gray-500">Параметры</Text>
            </Pressable>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
