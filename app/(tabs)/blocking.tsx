import React from 'react';
import { View, Text, SafeAreaView, FlatList, Pressable, Alert } from 'react-native';
import { MaterialCommunityIcons, Ionicons } from '@expo/vector-icons';
import { useGuardian } from '@/hooks/useGuardian';

export default function BlockingScreen() {
  const { blacklist, addToBlacklist, removeFromBlacklist } = useGuardian();

  const handleAddApp = () => {
    Alert.prompt(
      'Добавить приложение',
      'Введите название и пакет через запятую (напр. Telegram, org.telegram.messenger)',
      [
        { text: 'Отмена', style: 'cancel' },
        { 
          text: 'Добавить', 
          onPress: (value?: string) => {
            if (value) {
              const [name, packageName] = value.split(',').map((s: string) => s.trim());
              if (name && packageName) {
                addToBlacklist({ name, packageName });
              }
            }
          }
        }
      ]
    );
  };

  return (
    <SafeAreaView className="flex-1 bg-[#0a0b10]">
      <View className="px-6 py-4">
        <Text className="text-2xl font-bold text-white">Чёрный список</Text>
        <Text className="text-sm text-gray-400">Управление заблокированными приложениями</Text>
      </View>

      <FlatList
        data={blacklist}
        contentContainerStyle={{ padding: 24 }}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View className="mb-4 flex-row items-center rounded-3xl bg-[#12141c] p-4">
            <View className="mr-4 h-12 w-12 items-center justify-center rounded-2xl bg-[#1a1c26]">
              <MaterialCommunityIcons name="application" size={24} color="#94a3b8" />
            </View>
            <View className="flex-1">
              <Text className="text-base font-bold text-white">{item.name}</Text>
              <Text className="text-xs text-gray-500">{item.packageName}</Text>
            </View>
            <Pressable 
              onPress={() => removeFromBlacklist(item.id)}
              className="h-10 w-10 items-center justify-center rounded-full bg-[#2a1a1a]"
            >
              <MaterialCommunityIcons name="delete" size={20} color="#ef4444" />
            </Pressable>
          </View>
        )}
        ListEmptyComponent={
          <View className="items-center justify-center py-20">
            <MaterialCommunityIcons name="shield-off" size={64} color="#1a1c26" />
            <Text className="mt-4 text-gray-500">Список пуст</Text>
          </View>
        }
      />

      <Pressable 
        onPress={handleAddApp}
        className="absolute bottom-10 right-6 h-16 w-16 items-center justify-center rounded-full bg-[#6366f1] shadow-lg"
      >
        <Ionicons name="add" size={32} color="white" />
      </Pressable>
    </SafeAreaView>
  );
}
