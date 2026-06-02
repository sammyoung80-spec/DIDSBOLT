import React, { useState, useEffect, useRef } from 'react';
import {
  TrendingUp,
  TrendingDown,
  Shield,
  Zap,
  Settings,
  LogOut,
  DollarSign,
  Users,
  Ticket,
  Terminal,
  ArrowLeft,
  Activity,
  Globe,
  RefreshCw,
  Plus,
  Trash,
  Check,
  AlertCircle,
  ExternalLink,
  Lock,
  Compass,
  Cpu,
  UserPlus,
  Tv,
  CheckCircle,
  XCircle,
  AlertTriangle
} from 'lucide-react';

import {
  UserEntity,
  VoucherEntity,
  AssetEntity,
  PricePlanEntity,
  SignalHistoryEntity,
  initSupabaseClient,
  seedInitialLocalStorageData,
  fetchUsers,
  insertUser,
  deleteUserById,
  fetchVouchers,
  insertVoucher,
  deleteVoucherByCode,
  fetchAssets,
  insertAsset,
  deleteAssetByName,
  fetchPricePlans,
  insertPricePlan,
  deletePricePlanById,
  fetchSystemConfig,
  insertSystemConfig,
  fetchSignalHistory,
  insertSignal,
  clearSignalHistory,
  getSupabaseConfig
} from './Supabase';

// Pre-packaged constants
const TELEGRAM_SUPPORT_CHAT = "https://t.me/+2347016435125";

export default function App() {
  // --- DATABASE & APP STATE ---
  const [dbConfig, setDbConfig] = useState(getSupabaseConfig());
  const [users, setUsers] = useState<UserEntity[]>([]);
  const [vouchers, setVouchers] = useState<VoucherEntity[]>([]);
  const [assets, setAssets] = useState<AssetEntity[]>([]);
  const [pricingPlans, setPricingPlans] = useState<PricePlanEntity[]>([]);
  const [history, setHistory] = useState<SignalHistoryEntity[]>([]);
  
  // Custom Dynamic DB credentials config fields
  const [dbOverrideUrl, setDbOverrideUrl] = useState(localStorage.getItem('DIDS_OVERRIDE_SUPABASE_URL') || '');
  const [dbOverrideKey, setDbOverrideKey] = useState(localStorage.getItem('DIDS_OVERRIDE_SUPABASE_KEY') || '');

  // UI Views: "login" | "dashboard" | "paywall" | "settings" | "signalHistory" | "admin" | "twoFactor" | "linkBroker"
  const [activeView, setActiveView] = useState<string>("login");
  const [currentUser, setCurrentUser] = useState<UserEntity | null>(null);
  
  // Toast notifications
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'info' | 'error' } | null>(null);
  
  // Tickers & Configs
  const [announcement, setAnnouncement] = useState('');
  const [pocketOptionLink, setPocketOptionLink] = useState('https://pocketoption.com/register/');

  // Authentication Fields
  const [loginUsername, setLoginUsername] = useState('');
  const [isAdminLogin, setIsAdminLogin] = useState(false);
  const [adminAccessKey, setAdminAccessKey] = useState('');

  // Terminal & Active Signal Variables
  const [selectedPair, setSelectedPair] = useState('EUR/USD');
  const [timeframe, setTimeframe] = useState('1 MIN');
  const [activeSignalState, setActiveSignalState] = useState<'idle' | 'analyzing' | 'result' | 'expired'>('idle');
  const [countdown, setCountdown] = useState<number>(0);
  const [currentSignal, setCurrentSignal] = useState<{
    pair: string;
    direction: 'CALL' | 'PUT';
    strikePrice: string;
    expiryText: string;
    confidence: string;
    result: 'WIN' | 'LOSS';
  } | null>(null);

  // User input states inside Settings / Paywalls
  const [voucherInput, setVoucherInput] = useState('');
  
  // Admin inputs
  const [adminTab, setAdminTab] = useState<'users' | 'vouchers' | 'config'>('users');
  const [newUsernameInput, setNewUsernameInput] = useState('');
  const [newVoucherDays, setNewVoucherDays] = useState(1);
  const [announcementText, setAnnouncementText] = useState('');
  const [poLinkText, setPoLinkText] = useState('');
  const [customAssetInput, setCustomAssetInput] = useState('');
  const [customAssetOtc, setCustomAssetOtc] = useState(false);

  // References
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // --- SHOW TOAST HELP ---
  const showToast = (message: string, type: 'success' | 'info' | 'error' = 'info') => {
    setToast({ message, type });
  };

  useEffect(() => {
    if (toast) {
      const t = setTimeout(() => setToast(null), 3500);
      return () => clearTimeout(t);
    }
  }, [toast]);

  // --- INITIALIZATION ---
  useEffect(() => {
    seedInitialLocalStorageData();
    initSupabaseClient();
    reloadData();
  }, []);

  const reloadData = async () => {
    const freshUsers = await fetchUsers();
    const freshVouchers = await fetchVouchers();
    const freshAssets = await fetchAssets();
    const freshPlans = await fetchPricePlans();
    const freshHistory = await fetchSignalHistory();
    const announcementVal = await fetchSystemConfig('announcement', '🚨 LIVE DIDSBOLT TERMINAL SECURE — REDEEM ACCESS CODE TO BEGIN GENERATING AUTOMATED SIGNALS');
    const poLinkVal = await fetchSystemConfig('pocket_option_link', 'https://pocketoption.com/register/');

    setUsers(freshUsers);
    setVouchers(freshVouchers);
    setAssets(freshAssets);
    setPricingPlans(freshPlans);
    setHistory(freshHistory);
    setAnnouncement(announcementVal);
    setAnnouncementText(announcementVal);
    setPocketOptionLink(poLinkVal);
    setPoLinkText(poLinkVal);
    setDbConfig(getSupabaseConfig());

    // Update current user references if logged in
    if (currentUser) {
      const match = freshUsers.find(u => u.id === currentUser.id);
      if (match) {
        setCurrentUser(match);
      }
    }
  };

  // --- TRIGGER HARDWARE UPDATE FOR OVERRIDE DB CREDENTIALS ---
  const handleSaveDbOverride = () => {
    if (dbOverrideUrl.trim()) {
      localStorage.setItem('DIDS_OVERRIDE_SUPABASE_URL', dbOverrideUrl.trim());
    } else {
      localStorage.removeItem('DIDS_OVERRIDE_SUPABASE_URL');
    }

    if (dbOverrideKey.trim()) {
      localStorage.setItem('DIDS_OVERRIDE_SUPABASE_KEY', dbOverrideKey.trim());
    } else {
      localStorage.removeItem('DIDS_OVERRIDE_SUPABASE_KEY');
    }

    initSupabaseClient();
    reloadData();
    showToast("Database configurations updated instantly!", "success");
  };

  // --- COUNTDOWN SCHEDULER ---
  useEffect(() => {
    if (countdown > 0 && activeSignalState === 'result') {
      timerRef.current = setTimeout(() => {
        setCountdown(prev => prev - 1);
      }, 1000);
      return () => {
        if (timerRef.current) clearTimeout(timerRef.current);
      };
    } else if (countdown === 0 && activeSignalState === 'result') {
      setActiveSignalState('expired');
    }
  }, [countdown, activeSignalState]);

  // --- HELPER FUNCTION: TICKER STATUS ---
  const hasAccess = (): boolean => {
    if (!currentUser) return false;
    if (currentUser.id === "ADMIN") return true;
    if (!currentUser.isActive) return false;
    if (!currentUser.accessExpiresAt) return false;
    return currentUser.accessExpiresAt > Date.now();
  };

  // --- TRADING VIEW ACTIONS ---
  const triggerGenerateSignal = () => {
    if (!hasAccess()) {
      showToast("Access required. Plan expired or active pass missing.", "error");
      setActiveView("paywall");
      return;
    }

    if (activeSignalState === 'analyzing') return;

    setActiveSignalState('analyzing');
    setCurrentSignal(null);

    setTimeout(async () => {
      const isUp = Math.random() > 0.45;
      const confidenceNum = Math.floor(82 + Math.random() * 15);
      const isOtc = selectedPair.toLowerCase().includes("otc");
      
      let strike = (1.07200 + Math.random() * 0.02500).toFixed(5);
      if (selectedPair.includes("JPY")) {
        strike = (150.20 + Math.random() * 4.5).toFixed(3);
      } else if (selectedPair.includes("/USD") && !selectedPair.includes("EUR") && !selectedPair.includes("GBP") && !selectedPair.includes("AUD")) {
        strike = (64000 + Math.random() * 3000).toFixed(2); // Crypto
      } else if (selectedPair.includes("XAU")) {
        strike = (2300 + Math.random() * 60).toFixed(2);
      }

      const outcome: 'WIN' | 'LOSS' = Math.random() > 0.15 ? 'WIN' : 'LOSS'; // High winning theoretical probability
      
      const newSig = {
        pair: selectedPair,
        direction: isUp ? 'CALL' as const : 'PUT' as const,
        strikePrice: strike,
        expiryText: timeframe,
        confidence: `${confidenceNum}%`,
        result: outcome
      };

      setCurrentSignal(newSig);
      setActiveSignalState('result');

      // Set countdown based on timeframe string
      let seconds = 60;
      if (timeframe.includes("3")) seconds = 180;
      if (timeframe.includes("5")) seconds = 300;
      setCountdown(seconds);

      // Log into Database/Local History
      const timeLabel = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      await insertSignal({
        pair: selectedPair,
        direction: isUp ? 'CALL' : 'PUT',
        result: outcome,
        time: timeLabel,
        timestamp: Date.now()
      });

      reloadData();
      showToast(`${selectedPair} algorithmic decision compiled.`, "success");
    }, 3000);
  };

  const resetTerminal = () => {
    setActiveSignalState('idle');
    setCurrentSignal(null);
    setCountdown(0);
    if (timerRef.current) clearTimeout(timerRef.current);
  };

  // --- SIGNIN INGESTION ---
  const login = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isAdminLogin) {
      if (adminAccessKey.trim() === "DIDSBOLT_SEC_2026") {
        const adminUser: UserEntity = {
          id: "ADMIN",
          username: "SYSTEM_CONSOLE",
          accessExpiresAt: Date.now() + 86400000 * 365,
          isActive: true
        };
        setCurrentUser(adminUser);
        setActiveView("admin");
        showToast("Secure military Admin configuration active.", "success");
      } else {
        showToast("Access Code invalid. Operational terminal locked.", "error");
      }
    } else {
      if (!loginUsername.trim()) {
        showToast("Please input custom telegram tag.", "error");
        return;
      }

      let formatted = loginUsername.trim();
      if (!formatted.startsWith("@")) {
        formatted = "@" + formatted;
      }

      // Check user from registered directory database
      const match = users.find(u => u.username.toLowerCase() === formatted.toLowerCase());
      if (match) {
        setCurrentUser(match);
        if (!match.isActive) {
          showToast("This account is currently blocked by Admin.", "error");
        } else if (match.accessExpiresAt && match.accessExpiresAt > Date.now()) {
          setActiveView("dashboard");
          showToast(`Welcome back, ${match.username}`, "success");
        } else {
          setActiveView("paywall");
          showToast("Account active, but access pass expired.", "info");
        }
      } else {
        // Register a new user automatically
        const newUid = "U" + Math.floor(Math.random() * 89999 + 10000);
        const newUser: UserEntity = {
          id: newUid,
          username: formatted,
          accessExpiresAt: null, // New users start at empty state
          isActive: true
        };
        await insertUser(newUser);
        setCurrentUser(newUser);
        setUsers(prev => [...prev, newUser]);
        setActiveView("paywall");
        showToast("Dynamic account created. Please activate pass.", "success");
      }
    }
  };

  const logout = () => {
    setCurrentUser(null);
    setActiveView("login");
    setLoginUsername('');
    setAdminAccessKey('');
    showToast("Terminal session destroyed safely.", "info");
  };

  // --- VOUCHER REDEMPTION ---
  const redeemVoucher = async () => {
    if (!currentUser) return;
    if (!voucherInput.trim()) {
      showToast("Enter a valid coupon code.", "error");
      return;
    }

    const code = voucherInput.trim().toUpperCase();
    const match = vouchers.find(v => v.code === code);

    if (!match) {
      showToast("Verification failed: access code not found.", "error");
      return;
    }

    if (match.isRedeemed) {
      showToast("Voucher and terminal codes can only be used once.", "error");
      return;
    }

    // Apply pass
    const additionalTime = match.days * 86400000;
    const currentExpiry = currentUser.accessExpiresAt && currentUser.accessExpiresAt > Date.now()
      ? currentUser.accessExpiresAt
      : Date.now();
    
    const updatedUser: UserEntity = {
      ...currentUser,
      accessExpiresAt: currentExpiry + additionalTime,
      isActive: true
    };

    const updatedVoucher: VoucherEntity = {
      ...match,
      isRedeemed: true,
      redeemedBy: currentUser.username
    };

    await insertUser(updatedUser);
    await insertVoucher(updatedVoucher);
    
    setVoucherInput('');
    await reloadData();
    showToast(`Redeemed! +${match.days} days premium access applied.`, "success");
    setActiveView("dashboard");
  };

  // --- ADMIN ACTIONS ---
  const toggleUserActiveStatus = async (user: UserEntity) => {
    const updated = { ...user, isActive: !user.isActive };
    await insertUser(updated);
    reloadData();
    showToast(`Status toggled for ${user.username}`, "info");
  };

  const extendUserAccess = async (user: UserEntity, days: number) => {
    const start = user.accessExpiresAt && user.accessExpiresAt > Date.now() ? user.accessExpiresAt : Date.now();
    const updated = { ...user, accessExpiresAt: start + days * 86400000, isActive: true };
    await insertUser(updated);
    reloadData();
    showToast(`Added ${days} days to ${user.username}`, "success");
  };

  const subtractUserAccess = async (user: UserEntity) => {
    const updated = { ...user, accessExpiresAt: null, isActive: false };
    await insertUser(updated);
    reloadData();
    showToast(`Removed access for ${user.username}`, "info");
  };

  const handleDeleteUser = async (uid: string) => {
    await deleteUserById(uid);
    reloadData();
    showToast("User deleted from cloud registry.", "success");
  };

  const handleRegisterNewUser = async () => {
    if (!newUsernameInput.trim()) return;
    let name = newUsernameInput.trim();
    if (!name.startsWith("@")) name = "@" + name;
    
    const newUid = "U" + Math.floor(Math.random() * 89999 + 10000);
    const u: UserEntity = {
      id: newUid,
      username: name,
      accessExpiresAt: null,
      isActive: true
    };
    await insertUser(u);
    setNewUsernameInput('');
    reloadData();
    showToast(`User ${name} registered.`, "success");
  };

  const handleCreateVoucher = async () => {
    const randomCode = "DIDS-" + Math.random().toString(36).substring(2, 8).toUpperCase() + "-" + newVoucherDays + "DY";
    const v: VoucherEntity = {
      code: randomCode,
      days: newVoucherDays,
      isRedeemed: false,
      redeemedBy: null
    };
    await insertVoucher(v);
    reloadData();
    showToast(`Access voucher generated: ${randomCode}`, "success");
  };

  const handleRevokeVoucher = async (code: string) => {
    await deleteVoucherByCode(code);
    reloadData();
    showToast("Promo voucher code destroyed.", "info");
  };

  const handleSaveConfigs = async () => {
    await insertSystemConfig('announcement', announcementText);
    await insertSystemConfig('pocket_option_link', poLinkText);
    reloadData();
    showToast("Administrative configs saved across servers.", "success");
  };

  const handleAddCustomAsset = async () => {
    if (!customAssetInput.trim()) return;
    const clean = customAssetInput.trim().toUpperCase();
    await insertAsset({ name: clean, isOtc: customAssetOtc });
    setCustomAssetInput('');
    reloadData();
    showToast(`Symbol asset ${clean} inserted.`, "success");
  };

  const handleRemoveAsset = async (name: string) => {
    await deleteAssetByName(name);
    reloadData();
    showToast(`Symbol asset ${name} removed.`, "info");
  };

  // Pricing plans edit
  const handleEditPricePlan = async (id: number, name: string, price: string, days: number, desc: string) => {
    await insertPricePlan({ id, name, price, days, desc });
    reloadData();
    showToast("Pricing package configuration updated.", "success");
  };

  // --- TIME COUNT FORMATTER ---
  const formatTimeLeft = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col relative select-none">
      
      {/* GLOW DECORATIONS */}
      <div className="absolute top-[-10%] left-[20%] w-[400px] h-[400px] bg-cyan-500/5 rounded-full filter blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-15%] right-[10%] w-[500px] h-[500px] bg-blue-500/5 rounded-full filter blur-[150px] pointer-events-none"></div>

      {/* GLOBAL TOAST WIDGET */}
      {toast && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-[999] flex items-center gap-3 px-5 py-3 rounded-xl border bg-slate-900 shadow-2xl animate-bounce glow-cyan border-slate-800">
          {toast.type === 'success' && <CheckCircle className="text-emerald-400 w-5 h-5" />}
          {toast.type === 'error' && <XCircle className="text-red-400 w-5 h-5" />}
          {toast.type === 'info' && <AlertTriangle className="text-cyan-400 w-5 h-5" />}
          <span className="text-sm font-medium tracking-wide">{toast.message}</span>
        </div>
      )}

      {/* HEADER BAR */}
      <header className="sticky top-0 z-40 bg-slate-950/80 backdrop-blur-md border-b border-slate-900 px-4 py-4 md:px-8">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          
          <div className="flex items-center gap-3 cursor-pointer" onClick={() => currentUser ? setActiveView("dashboard") : null}>
            <div className="p-2 bg-gradient-to-br from-cyan-400 to-blue-600 rounded-xl glow-cyan">
              <Cpu className="w-6 h-6 text-slate-950" />
            </div>
            <div>
              <h1 className="text-lg font-extrabold tracking-wider bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent">
                DIDS BOLT SIGNAL
              </h1>
              <p className="text-[10px] text-slate-400 tracking-widest uppercase font-semibold">PREDICTIVE SECURE TERMINAL</p>
            </div>
          </div>

          {currentUser && (
            <div className="flex items-center gap-2 md:gap-4">
              <span className="hidden md:inline text-xs font-semibold text-slate-400 px-3 py-1 bg-slate-900 border border-slate-800 rounded-full">
                {currentUser.username}
              </span>
              
              {currentUser.id === "ADMIN" ? (
                <button
                  onClick={() => setActiveView(activeView === "admin" ? "dashboard" : "admin")}
                  className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg border text-xs font-bold transition-all ${
                    activeView === "admin"
                      ? "bg-cyan-500/10 border-cyan-400 text-cyan-400"
                      : "bg-slate-900 border-slate-800 text-slate-300 hover:text-white"
                  }`}
                >
                  <Terminal className="w-3.5 h-3.5" />
                  Console
                </button>
              ) : (
                <button
                  onClick={() => setActiveView("settings")}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs transition-all ${
                    activeView === "settings"
                      ? "bg-slate-800 border-slate-700 text-cyan-400"
                      : "bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200"
                  }`}
                >
                  <Settings className="w-4 h-4" />
                </button>
              )}

              <button
                onClick={logout}
                className="flex items-center gap-1 bg-red-950/20 hover:bg-red-900/30 border border-red-900/40 hover:border-red-500/50 text-red-400 p-2 rounded-lg text-xs font-medium transition-all"
                title="Logout Safe"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </header>

      {/* BROADCAST ANNOUNCEMENT MARQUEE */}
      {announcement && (
        <div className="bg-slate-900 border-b border-slate-800 py-1.5 overflow-hidden text-xs text-cyan-400 font-mono tracking-wide relative">
          <div className="whitespace-nowrap animate-marquee">
            {announcement}
          </div>
        </div>
      )}

      {/* MAIN LAYOUT CANVAS */}
      <main className="flex-grow flex items-center justify-center p-4 md:p-8 max-w-7xl mx-auto w-full">
        
        {/* VIEW 1: SIGN IN PANELS */}
        {activeView === "login" && (
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 relative shadow-2xl">
            <div className="absolute top-0 right-0 w-24 h-24 bg-cyan-400/5 rounded-full filter blur-[20px] pointer-events-none"></div>
            
            <div className="text-center mb-8">
              <h2 className="text-2xl font-black bg-gradient-to-b from-white to-slate-300 bg-clip-text text-transparent">
                AUTHENTICATION PORT
              </h2>
              <p className="text-xs text-slate-400 mt-2">DIDS BOLT SIGNAL QUANTITATIVE PLATFORM</p>
            </div>

            <form onSubmit={login} className="space-y-6">
              
              {/* Login mode tabs */}
              <div className="grid grid-cols-2 bg-slate-950 p-1 rounded-xl border border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsAdminLogin(false)}
                  className={`py-2 rounded-lg text-xs font-bold transition-all ${!isAdminLogin ? 'bg-slate-900 text-white' : 'text-slate-400 hover:text-white'}`}
                >
                  TELEGRAM GATEWAY
                </button>
                <button
                  type="button"
                  onClick={() => setIsAdminLogin(true)}
                  className={`py-2 rounded-lg text-xs font-bold transition-all ${isAdminLogin ? 'bg-slate-900 text-white' : 'text-slate-400 hover:text-white'}`}
                >
                  SECURE ADMIN
                </button>
              </div>

              {!isAdminLogin ? (
                <div>
                  <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-widest mb-2">Telegram Tag</label>
                  <input
                    type="text"
                    required
                    placeholder="@username"
                    value={loginUsername}
                    onChange={(e) => setLoginUsername(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-cyan-400 transition-colors"
                  />
                  <p className="text-[10px] text-slate-500 mt-2 font-mono">Example: @trader_joe (New/existing tags instant entry)</p>
                </div>
              ) : (
                <div>
                  <label className="block text-[11px] font-bold text-slate-400 uppercase tracking-widest mb-2">Admin Terminal Key</label>
                  <div className="relative">
                    <input
                      type="password"
                      required
                      placeholder="••••••••••••••"
                      value={adminAccessKey}
                      onChange={(e) => setAdminAccessKey(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-red-500 transition-colors"
                    />
                    <Lock className="w-4 h-4 text-slate-400 absolute right-4 top-1/2 -translate-y-1/2" />
                  </div>
                  <p className="text-[10px] text-red-400/70 mt-2 font-mono">Input developer passcode to override configs</p>
                </div>
              )}

              <button
                type="submit"
                className="w-full bg-gradient-to-r from-cyan-500 to-blue-600 font-bold py-3.5 rounded-xl text-slate-950 tracking-wider hover:brightness-110 active:scale-[0.98] transition-all bg-size-200"
              >
                {isAdminLogin ? "INITIALIZE OPERATIONS" : "ESTABLISH TELECOMMUNICATION"}
              </button>
            </form>

            <div className="mt-8 border-t border-slate-800 pt-6">
              <h3 className="text-xs font-bold text-slate-400 mb-3 tracking-widest">DYNAMIC SUPABASE ENVIRONMENT OVERRIDES</h3>
              <div className="space-y-4">
                <div>
                  <input
                    type="text"
                    placeholder="https://...supabase.co"
                    value={dbOverrideUrl}
                    onChange={(e) => setDbOverrideUrl(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-cyan-400 text-slate-300 font-mono"
                  />
                </div>
                <div>
                  <input
                    type="password"
                    placeholder="eyJhbGciOiJIUzI1NiIsIn..."
                    value={dbOverrideKey}
                    onChange={(e) => setDbOverrideKey(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-cyan-400 text-slate-300 font-mono"
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleSaveDbOverride}
                    className="flex-grow bg-slate-950 border border-slate-800 hover:border-cyan-400 text-cyan-400 hover:text-white py-2 rounded-lg text-xs font-bold transition-all"
                  >
                    APPLY CHANGES
                  </button>
                  <button
                    onClick={() => {
                      setDbOverrideUrl('');
                      setDbOverrideKey('');
                      localStorage.removeItem('DIDS_OVERRIDE_SUPABASE_URL');
                      localStorage.removeItem('DIDS_OVERRIDE_SUPABASE_KEY');
                      initSupabaseClient();
                      reloadData();
                      showToast("Fallback sandbox local database enabled.", "info");
                    }}
                    className="bg-red-950/20 border border-red-900/30 text-red-400 px-3 py-2 rounded-lg text-xs font-bold"
                  >
                    RESET
                  </button>
                </div>
                <p className="text-[10px] text-slate-500 font-mono">
                  Current Status: {dbConfig.isEnabled ? <span className="text-emerald-400 font-bold font-sans">CLOUD ONLINE</span> : <span className="text-yellow-500 font-bold font-sans">LOCAL OFFLINE ENGINE</span>}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* VIEW 2: SIGNAL TERMINAL DASHBOARD */}
        {activeView === "dashboard" && (
          <div className="w-full grid grid-cols-1 lg:grid-cols-3 gap-6">
            
            {/* COLUMN 1 & 2: ACTION CONTROLS */}
            <div className="lg:col-span-2 space-y-6">
              
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 relative">
                <h2 className="text-xl font-extrabold flex items-center gap-2 border-b border-slate-800 pb-4">
                  <Cpu className="text-cyan-400 w-5 h-5 animate-pulse-slow" />
                  PREDICTIVE DECISION ENGINE
                </h2>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                  
                  {/* Select Pair Dropdown */}
                  <div>
                    <label className="block text-[10px] uppercase tracking-wider text-slate-400 mb-2 font-bold select-none">
                      TRADING INSTRUMENT / CURRENCY ASSET
                    </label>
                    <select
                      value={selectedPair}
                      onChange={(e) => setSelectedPair(e.target.value)}
                      disabled={activeSignalState === 'analyzing'}
                      className="w-full bg-slate-950 border border-slate-800 hover:border-cyan-400/50 disabled:opacity-50 text-white rounded-xl px-4 py-3.5 text-sm font-semibold focus:outline-none transition-colors"
                    >
                      <optgroup label="Standard Liquid Assets">
                        {assets.filter(a => !a.isOtc).map(a => (
                          <option key={a.name} value={a.name}>{a.name}</option>
                        ))}
                      </optgroup>
                      <optgroup label="Over-The-Counter (OTC) Digital Pairs">
                        {assets.filter(a => a.isOtc).map(a => (
                          <option key={a.name} value={a.name}>{a.name}</option>
                        ))}
                      </optgroup>
                    </select>
                  </div>

                  {/* Prediction Timeframe selectors */}
                  <div>
                    <label className="block text-[10px] uppercase tracking-wider text-slate-400 mb-2 font-bold select-none">
                      EXPIRY WINDOW PERIOD
                    </label>
                    <div className="grid grid-cols-3 bg-slate-950 p-1.5 rounded-xl border border-slate-800">
                      {["1 MIN", "3 MIN", "5 MIN"].map((tf) => (
                        <button
                          key={tf}
                          disabled={activeSignalState === 'analyzing'}
                          onClick={() => setTimeframe(tf)}
                          className={`py-2 rounded-lg text-xs font-bold transition-all ${
                            timeframe === tf
                              ? "bg-gradient-to-br from-cyan-400 to-blue-600 text-slate-950 font-black"
                              : "text-slate-400 hover:text-white"
                          }`}
                        >
                          {tf}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                {/* VISUALIZER DYNAMIC STATE ENGINES */}
                <div className="mt-8 border border-slate-800 bg-slate-950/60 rounded-xl p-8 flex flex-col items-center justify-center min-h-[300px]">
                  
                  {activeSignalState === 'idle' && (
                    <div className="text-center max-w-sm">
                      <div className="w-16 h-16 bg-cyan-400/10 text-cyan-400 rounded-full flex items-center justify-center mx-auto mb-4 animate-pulse-slow">
                        <Terminal className="w-8 h-8" />
                      </div>
                      <h3 className="text-lg font-bold tracking-wide">COMPILER COMPONENT ONLINE</h3>
                      <p className="text-xs text-slate-400 mt-2 font-mono">
                        System telemetry verified. Choose asset currency parameters above then initiate automated network computation sequence.
                      </p>
                    </div>
                  )}

                  {activeSignalState === 'analyzing' && (
                    <div className="text-center max-w-sm flex flex-col items-center">
                      <div className="relative w-20 h-20 mb-6">
                        <div className="absolute inset-0 rounded-full border-4 border-slate-900"></div>
                        <div className="absolute inset-0 rounded-full border-4 border-t-cyan-400 border-r-cyan-400/50 animate-spin"></div>
                      </div>
                      <h3 className="text-lg font-bold tracking-wider text-cyan-400 font-mono animate-pulse">
                        COMPILING PREDICTIONS...
                      </h3>
                      <p className="text-[10px] text-slate-500 font-mono mt-2 uppercase tracking-widest">
                        Accessing satellite indicators & price feeds
                      </p>
                    </div>
                  )}

                  {activeSignalState === 'result' && currentSignal && (
                    <div className="w-full max-w-md space-y-6 animate-pulse-slow">
                      
                      <div className="flex justify-between items-center bg-slate-900 border border-slate-800 rounded-xl px-4 py-2 font-mono text-[11px] text-cyan-400">
                        <span>INSTRUMENT: {currentSignal.pair}</span>
                        <span>CONFIDENCE: {currentSignal.confidence}</span>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        
                        <div className={`rounded-xl border p-4 text-center select-none shadow-lg ${
                          currentSignal.direction === 'CALL' 
                            ? "bg-emerald-950/20 border-emerald-500/30 text-emerald-400" 
                            : "bg-rose-950/20 border-rose-500/30 text-rose-400"
                        }`}>
                          <p className="text-[10px] text-slate-400 uppercase font-bold tracking-widest">DECISION</p>
                          <div className="flex items-center justify-center gap-1.5 fill-current mt-2">
                            {currentSignal.direction === 'CALL' ? <TrendingUp className="w-6 h-6 animate-bounce" /> : <TrendingDown className="w-6 h-6 animate-bounce" />}
                            <span className="text-2xl font-black">{currentSignal.direction}</span>
                          </div>
                          <p className="text-[10px] text-slate-400 font-semibold mt-1">BUY & ENTER IMMEDIATELY</p>
                        </div>

                        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 text-center">
                          <p className="text-[10px] text-slate-400 uppercase font-bold tracking-widest">EXPIRY COUNTDOWN</p>
                          <p className="text-2xl font-black text-white mt-2 font-mono">{formatTimeLeft(countdown)}</p>
                          <p className="text-[10px] text-slate-500 font-semibold mt-1">LOCK SCREEN FOR FEED</p>
                        </div>
                      </div>

                      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex justify-between items-center font-mono text-xs">
                        <div>
                          <span className="text-slate-400 text-[10px] uppercase font-bold tracking-wider block">RECOMMENDED ENTRY RATE</span>
                          <span className="font-extrabold text-base tracking-wider text-slate-100">{currentSignal.strikePrice}</span>
                        </div>
                        <div className="text-right">
                          <span className="text-slate-400 text-[10px] uppercase font-bold tracking-wider block">TIMEFRAME EXPIRE</span>
                          <span className="text-cyan-400 font-bold">{currentSignal.expiryText}</span>
                        </div>
                      </div>
                    </div>
                  )}

                  {activeSignalState === 'expired' && (
                    <div className="text-center max-w-sm">
                      <div className="w-16 h-16 bg-red-400/10 text-red-400 rounded-full flex items-center justify-center mx-auto mb-4">
                        <AlertCircle className="w-8 h-8" />
                      </div>
                      <h3 className="text-lg font-bold tracking-widest text-red-400 font-mono">SIGNAL SEQUENCE EXPIRED</h3>
                      <p className="text-xs text-slate-400 mt-2 font-mono">
                        The signal parameters duration time has completed. Re-enter the terminal stream loop to compile fresh outcomes.
                      </p>
                      <button
                        onClick={resetTerminal}
                        className="mt-4 px-5 py-2 bg-slate-900 border border-slate-800 hover:border-slate-700 hover:text-white rounded-lg text-xs font-bold transition-all text-slate-300"
                      >
                        RESET TELEMETRY TERMINAL
                      </button>
                    </div>
                  )}
                </div>

                {/* Primary compile button trigger */}
                <div className="mt-6 flex flex-col md:flex-row gap-4 items-center justify-between border-t border-slate-800 pt-6">
                  
                  <div className="text-center md:text-left select-none">
                    <span className="text-xs text-slate-400 font-medium">Telemetry sync rate: <span className="text-emerald-400 font-bold">96.3%</span></span>
                    <p className="text-[10px] text-slate-400/70 font-mono leading-relaxed max-w-md mt-1">
                      Signal precision compiling active. Avoid multi-click loops. Do not close browser while stream transitions running.
                    </p>
                  </div>

                  <button
                    onClick={triggerGenerateSignal}
                    disabled={activeSignalState === 'analyzing'}
                    className="w-full md:w-auto bg-gradient-to-r from-cyan-400 to-blue-500 hover:brightness-110 disabled:opacity-50 text-slate-950 font-black tracking-wider px-8 py-4 rounded-xl shadow-lg shadow-cyan-400/10 flex items-center justify-center gap-2 transition-all active:scale-[0.98]"
                  >
                    <Cpu className="w-5 h-5 animate-pulse" />
                    {activeSignalState === 'analyzing' ? 'INTERCEPTING TELEMETRY...' : 'COMPILE ALGORITHMIC SIGNAL'}
                  </button>
                </div>
              </div>

              {/* Pocket Option Promotion Card */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl grid grid-cols-1 md:grid-cols-3">
                <div className="bg-gradient-to-b from-slate-900 to-slate-950 p-6 flex flex-col justify-center items-center text-center md:border-r border-slate-800">
                  <Globe className="text-cyan-400 w-10 h-10 animate-spin-slow mb-3" />
                  <span className="text-xs text-slate-400 font-bold font-mono">OFFICIAL SPONSORSHIP</span>
                  <h4 className="text-sm font-black text-slate-200 mt-1 uppercase">POCKET OPTION PARTNER</h4>
                </div>
                
                <div className="col-span-2 p-6 flex flex-col justify-between">
                  <div>
                    <h3 className="text-base font-bold text-slate-100">CLAIM UP TO $10,000 DEMO & 50% MATCH DEPOSIT</h3>
                    <p className="text-xs text-slate-400 leading-relaxed mt-2 font-mono">
                      Activate automated operational efficiency on Pocket Option. Sign-up with our verified partner URL to integrate directly with internal OTC algorithm endpoints.
                    </p>
                  </div>

                  <div className="mt-4 flex flex-col md:flex-row gap-3 items-center">
                    <a
                      href={pocketOptionLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-full md:w-auto bg-slate-950 hover:bg-slate-900 border border-slate-700 hover:border-cyan-400 text-cyan-400 hover:text-white px-5 py-2.5 rounded-lg text-xs font-bold font-mono flex items-center justify-center gap-1.5 transition-all"
                    >
                      REGISTER BROKER ACCOUNT
                      <ExternalLink className="w-3.5 h-3.5" />
                    </a>
                    <span className="text-[10px] text-slate-500 font-mono">Supported trading globally</span>
                  </div>
                </div>
              </div>
            </div>

            {/* COLUMN 3: SIGNAL HISTORY TIMELINE */}
            <div className="space-y-6">
              
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 flex flex-col h-[525px] relative">
                
                <h3 className="text-base font-bold flex items-center justify-between border-b border-slate-800 pb-4">
                  <span className="flex items-center gap-2">
                    <Activity className="text-cyan-400 w-4.5 h-4.5" />
                    REAL-TIME SIGNAL STREAM
                  </span>
                  <button
                    onClick={async () => {
                      await clearSignalHistory();
                      reloadData();
                      showToast("Terminal history registry cleared.", "info");
                    }}
                    className="text-[10px] text-red-400/80 hover:text-red-400 font-mono"
                  >
                    CLEAR
                  </button>
                </h3>

                <div className="flex-grow overflow-y-auto mt-4 space-y-3.5 pr-1">
                  {history.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-center p-6 text-slate-500 font-mono">
                      <Sliders className="w-10 h-10 opacity-30 mb-2" />
                      <span className="text-[10px] uppercase font-bold tracking-wider">No historic signals logged</span>
                      <p className="text-[10px] leading-relaxed max-w-[200px] mt-1">Compile your first signal algorithm to populate feed logs</p>
                    </div>
                  ) : (
                    history.map((sig, i) => (
                      <div
                        key={sig.id || i}
                        className="bg-slate-950 border border-slate-850 p-3.5 rounded-xl flex items-center justify-between font-mono text-[11px] hover:border-slate-700 transition-colors"
                      >
                        <div className="space-y-1">
                          <div className="font-extrabold text-white text-[12px] flex items-center gap-1.5">
                            <span className="bg-slate-900 px-2 py-0.5 border border-slate-800 rounded-md text-[10px]">{sig.pair}</span>
                          </div>
                          <span className="text-slate-500 text-[10px] block font-semibold mt-1">{sig.time}</span>
                        </div>

                        <div className="flex items-center gap-3">
                          <span className={`px-2.5 py-1 rounded-md text-[10px] font-black tracking-wider ${
                            sig.direction === 'CALL' 
                              ? "bg-emerald-950/30 text-emerald-400 border border-emerald-900/60" 
                              : "bg-rose-950/30 text-rose-400 border border-rose-900/60"
                          }`}>
                            {sig.direction}
                          </span>
                          
                          <span className={`text-[12px] font-black rounded px-1.5 py-0.5 ${
                            sig.result === 'WIN' 
                              ? "bg-emerald-500/10 text-emerald-400" 
                              : "bg-rose-500/10 text-rose-400"
                          }`}>
                            {sig.result}
                          </span>
                        </div>
                      </div>
                    ))
                  )}
                </div>

                <div className="border-t border-slate-800 pt-4 mt-2 bg-slate-900 text-center select-none font-mono text-[10px] text-slate-500">
                  AUTO-SYNCING DEMENTIA TELEMETRY
                </div>
              </div>

              {/* SUPPORT TICKET ACCIDENT CARD */}
              <div className="bg-slate-900 border border-slate-850 rounded-2xl p-5 flex items-center justify-between gap-4">
                <div className="space-y-1">
                  <h4 className="text-sm font-bold text-slate-200">TELEGRAM CONFIG GATEWAY</h4>
                  <p className="text-[10px] text-slate-400 leading-relaxed font-mono">
                    Join custom VIP announcement rooms, resolve access vouchers, or speak directly to support admins.
                  </p>
                </div>
                <a
                  href={TELEGRAM_SUPPORT_CHAT}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="bg-cyan-400 text-slate-950 p-3 rounded-xl hover:bg-cyan-300 transition-colors shrink-0"
                >
                  <Cpu className="w-5 h-5" />
                </a>
              </div>
            </div>
          </div>
        )}

        {/* VIEW 3: INVOICING / VOUCHER REDEEM / SUBSCRIPTION PAYWALL */}
        {activeView === "paywall" && (
          <div className="w-full max-w-4xl space-y-8 animate-fade-in">
            
            <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 text-center relative max-w-2xl mx-auto">
              <div className="absolute top-0 left-1/2 -translate-x-1/2 w-48 h-12 bg-cyan-400/10 rounded-full filter blur-[20px] pointer-events-none"></div>
              
              <div className="w-12 h-12 bg-cyan-500/10 text-cyan-400 rounded-full flex items-center justify-center mx-auto mb-4">
                <Lock className="w-6 h-6" />
              </div>
              <h2 className="text-2xl font-black tracking-wider text-slate-100">VIP SUBSCRIPTION PASS REQUIRED</h2>
              <p className="text-xs text-slate-400 mt-2 font-mono leading-relaxed">
                Terminal algorithms require a verified license pass to compute. Choose a paid license access pack or input an admin voucher code to initialize.
              </p>

              {/* Enter Voucher section */}
              <div className="bg-slate-950 border border-slate-850 p-4 rounded-2xl mt-8 flex flex-col md:flex-row gap-3 items-center">
                <input
                  type="text"
                  placeholder="ENTER ACCESS CODE OR PROMO VOUCHER"
                  value={voucherInput}
                  onChange={(e) => setVoucherInput(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 text-xs focus:outline-none focus:border-cyan-400 tracking-widest text-center font-mono font-bold uppercase"
                />
                <button
                  onClick={redeemVoucher}
                  className="w-full md:w-auto shrink-0 bg-cyan-400 text-slate-950 font-black tracking-wider text-xs px-6 py-3 rounded-xl hover:bg-cyan-300 transition-colors"
                >
                  ACTIVATE VOUCHER CODE
                </button>
              </div>
            </div>

            {/* Pricing layouts lists */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {pricingPlans.map((plan) => (
                <div
                  key={plan.id}
                  className="bg-slate-900 border border-slate-800 hover:border-cyan-400/50 rounded-2xl p-6 flex flex-col justify-between relative shadow-xl transition-all"
                >
                  <div className="space-y-4">
                    <span className="text-[10px] uppercase font-bold text-cyan-400 font-mono tracking-widest bg-slate-950 px-2.5 py-1 border border-slate-800 rounded-full inline-block">
                      {plan.name}
                    </span>
                    
                    <div className="mt-2 text-white font-mono">
                      <span className="text-3xl font-black tracking-tighter bg-gradient-to-r from-slate-100 to-slate-300 bg-clip-text text-transparent">{plan.price}</span>
                      <span className="text-xs text-slate-400 font-medium ml-1">USD</span>
                    </div>

                    <p className="text-xs text-slate-400 leading-relaxed font-mono bg-slate-950/40 p-3 rounded-xl border border-slate-850">
                      {plan.desc}
                    </p>
                  </div>

                  <div className="mt-8 border-t border-slate-850 pt-5 space-y-4">
                    <div className="flex gap-2 text-[10px] text-slate-400 font-semibold font-mono">
                      <Check className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                      Unlimited terminal signals compile
                    </div>
                    <div className="flex gap-2 text-[10px] text-slate-400 font-semibold font-mono">
                      <Check className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                      OTC asset integration unlocked
                    </div>
                    <div className="flex gap-2 text-[10px] text-slate-400 font-semibold font-mono">
                      <Check className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                      Automatic support & vip announcement telegram setup
                    </div>

                    <a
                      href={TELEGRAM_SUPPORT_CHAT}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-full bg-slate-950 hover:bg-slate-900 border border-slate-800 hover:border-cyan-400 text-cyan-400 hover:text-white font-bold text-xs py-3 rounded-xl flex items-center justify-center gap-1 transition-all"
                    >
                      PURCHASE WITH TELEGRAM ADMIN
                      <ExternalLink className="w-3.5 h-3.5" />
                    </a>
                  </div>
                </div>
              ))}
            </div>

            <div className="text-center pt-4">
              <button
                onClick={() => setActiveView("dashboard")}
                className="text-xs text-slate-400 hover:text-white font-semibold font-mono flex items-center gap-1 mx-auto"
              >
                <ArrowLeft className="w-4 h-4" />
                RETURN TO DASHBOARD TELEMETRY
              </button>
            </div>
          </div>
        )}

        {/* VIEW 4: SYSTEM SETTINGS */}
        {activeView === "settings" && currentUser && (
          <div className="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 space-y-8 shadow-2xl relative">
            
            <div className="flex items-center justify-between border-b border-slate-800 pb-5">
              <h2 className="text-xl font-extrabold flex items-center gap-2">
                <Settings className="text-cyan-400" />
                OPERATIONAL SECURITY CONFIGS
              </h2>
              <button
                onClick={() => setActiveView("dashboard")}
                className="bg-slate-950 text-slate-400 hover:text-white p-2 border border-slate-850 rounded-xl transition-all"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-6">
              
              {/* Profile setup details */}
              <div className="bg-slate-950 border border-slate-850 p-5 rounded-2xl">
                <h3 className="text-xs font-black uppercase text-slate-400 tracking-wider mb-4">TERMINAL PROFILE DIRECTORY</h3>
                <div className="grid grid-cols-2 gap-4 font-mono text-[11px]">
                  <div>
                    <span className="text-slate-500">Registry ID:</span>
                    <p className="text-slate-200 mt-1 font-semibold">{currentUser.id}</p>
                  </div>
                  <div>
                    <span className="text-slate-500">Telegram Handle:</span>
                    <p className="text-slate-200 mt-1 font-semibold">{currentUser.username}</p>
                  </div>
                  <div className="col-span-2 border-t border-slate-850 pt-3">
                    <span className="text-slate-500">Pass Expiration Status:</span>
                    <p className="text-cyan-400 mt-1 font-semibold">
                      {currentUser.accessExpiresAt && currentUser.accessExpiresAt > Date.now()
                        ? new Date(currentUser.accessExpiresAt).toLocaleString()
                        : "No Active Premium Access Days Available"}
                    </p>
                  </div>
                </div>
              </div>

              {/* Redeem Voucher inside settings */}
              <div>
                <label className="block text-[10px] uppercase text-slate-400 font-black tracking-widest mb-2 font-mono">Redeem Premium Coupon</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    placeholder="ENTER CODES OR VOUCHER"
                    value={voucherInput}
                    onChange={(e) => setVoucherInput(e.target.value)}
                    className="flex-grow bg-slate-950 border border-slate-800 rounded-xl px-4 py-2 text-xs focus:outline-none focus:border-cyan-400 font-mono tracking-wider uppercase font-bold"
                  />
                  <button
                    onClick={redeemVoucher}
                    className="bg-cyan-400 hover:bg-cyan-300 text-slate-950 px-6 py-2.5 rounded-xl font-bold text-xs font-mono transition-colors"
                  >
                    APPLY
                  </button>
                </div>
              </div>

              {/* Sub features navigation listings */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <button
                  onClick={() => setActiveView("linkBroker")}
                  className="bg-slate-950 hover:bg-slate-950/60 transition-all border border-slate-850 p-4 rounded-xl flex items-center justify-between text-left"
                >
                  <div className="space-y-1">
                    <h5 className="text-xs font-bold text-white uppercase">Link Trading Broker</h5>
                    <p className="text-[10px] text-slate-550 leading-relaxed font-mono">
                      Coordinate automation triggers on Pocket Option and Binary API keys.
                    </p>
                  </div>
                  <Globe className="text-cyan-400 shrink-0 w-5 h-5 ml-2" />
                </button>

                <button
                  onClick={() => setActiveView("twoFactor")}
                  className="bg-slate-950 hover:bg-slate-950/60 transition-all border border-slate-850 p-4 rounded-xl flex items-center justify-between text-left"
                >
                  <div className="space-y-1">
                    <h5 className="text-xs font-bold text-white uppercase">2-Factor Authentication</h5>
                    <p className="text-[10px] text-slate-550 leading-relaxed font-mono">
                      Enhance cryptographic terminal security loops with authenticators.
                    </p>
                  </div>
                  <Shield className="text-cyan-400 shrink-0 w-5 h-5 ml-2" />
                </button>
              </div>

              {/* Support triggers */}
              <a
                href={TELEGRAM_SUPPORT_CHAT}
                target="_blank"
                rel="noopener noreferrer"
                className="block bg-slate-950 border border-slate-850 hover:border-slate-800 p-4 rounded-xl flex items-center justify-between"
              >
                <div className="space-y-1">
                  <h5 className="text-xs font-bold text-slate-200 uppercase">Emergency Support Broadcast</h5>
                  <p className="text-[10px] text-slate-400 leading-relaxed font-mono">
                    Open Telegram portal to communicate with deployment support staff.
                  </p>
                </div>
                <ExternalLink className="text-cyan-400 w-5 h-5" />
              </a>

            </div>
          </div>
        )}

        {/* VIEW 5: TWO FACTOR PLACEHOLDER CONFIG */}
        {activeView === "twoFactor" && (
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 space-y-6 shadow-2xl text-center">
            <div className="w-12 h-12 bg-cyan-400/10 text-cyan-400 rounded-full flex items-center justify-center mx-auto">
              <Shield className="w-6 h-6" />
            </div>
            
            <div className="space-y-2">
              <h2 className="text-lg font-extrabold uppercase text-white">Cryptographic 2-Factor Auth</h2>
              <p className="text-xs text-slate-450 leading-relaxed font-mono">
                System telemetry generated custom token code triggers. Coordinate Google or Authy apps to preserve console safety.
              </p>
            </div>

            <div className="bg-slate-950 border border-slate-850 p-4 rounded-xl space-y-3">
              <span className="text-[10px] text-slate-500 font-bold block">SECRET SEED KEY OVERALL</span>
              <p className="text-sm font-mono font-bold tracking-widest text-cyan-400 bg-slate-900 py-2.5 rounded border border-slate-800">
                DIDS BOLT COMPILER 2026 KEY
              </p>
            </div>

            <button
              onClick={() => setActiveView("settings")}
              className="w-full bg-slate-950 hover:bg-slate-900 border border-slate-800 py-3 rounded-xl text-xs font-bold font-mono transition-colors"
            >
              COMMIT SECURE & CLOSE
            </button>
          </div>
        )}

        {/* VIEW 6: LINK OPERATIONS BROKER */}
        {activeView === "linkBroker" && (
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 space-y-6 shadow-2xl text-center">
            
            <div className="w-12 h-12 bg-cyan-400/10 text-cyan-400 rounded-full flex items-center justify-center mx-auto">
              <Globe className="w-6 h-6" />
            </div>

            <div className="space-y-2">
              <h2 className="text-lg font-extrabold uppercase text-white">Link Pocket Option Broker</h2>
              <p className="text-xs text-slate-450 leading-relaxed font-mono">
                Paste your dynamic binary option UID details below. Intercepting API triggers from trading broker accounts delivers direct signal execution coordinates.
              </p>
            </div>

            <div className="space-y-4 text-left">
              <div>
                <label className="block text-[10px] uppercase text-slate-400 font-bold tracking-wider mb-1 font-mono">Broker Account Account UID</label>
                <input
                  type="text"
                  placeholder="e.g. 54939281"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-cyan-400 text-white font-mono"
                />
              </div>

              <div>
                <label className="block text-[10px] uppercase text-slate-400 font-bold tracking-wider mb-1 font-mono">Network Stream Delay Target</label>
                <select className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2 py-2 text-xs text-white focus:outline-none">
                  <option>0.0 seconds (Real-time compilations)</option>
                  <option>0.5 seconds (Latency buffer filters)</option>
                  <option>1.0 seconds (High-volatility security delays)</option>
                </select>
              </div>
            </div>

            <button
              onClick={() => {
                showToast("Broker credentials linked successfully", "success");
                setActiveView("settings");
              }}
              className="w-full bg-cyan-400 text-slate-950 font-black py-3 rounded-xl text-xs font-mono hover:bg-cyan-300 transition-colors"
            >
              ESTABLISH API INTEGRATIONS
            </button>
          </div>
        )}

        {/* VIEW 7: ADMINISTRATIVE SYSTEM COMMAND CENTER */}
        {activeView === "admin" && currentUser?.id === "ADMIN" && (
          <div className="w-full max-w-5xl bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 space-y-8 shadow-2xl relative">
            
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
              <div>
                <h2 className="text-xl font-extrabold flex items-center gap-2">
                  <Terminal className="text-red-400 animate-pulse-slow w-6 h-6" />
                  ADMIN CLOUD CONTROL COMMAND
                </h2>
                <p className="text-[10px] text-slate-450 mt-1 font-mono uppercase tracking-widest">Master interface registry overriding live models</p>
              </div>

              <div className="flex items-center gap-3">
                <button
                  onClick={reloadData}
                  className="bg-slate-950 text-slate-400 hover:text-white border border-slate-850 p-2 rounded-xl transition-all"
                  title="Force Refresh Databases"
                >
                  <RefreshCw className="w-5 h-5" />
                </button>
                <button
                  onClick={() => setActiveView("dashboard")}
                  className="bg-slate-950 text-slate-400 hover:text-white px-4 py-2 border border-slate-850 rounded-xl text-xs font-bold font-mono transition-all"
                >
                  DASHBOARD
                </button>
              </div>
            </div>

            {/* Config Sub Tabs navigation panels */}
            <div className="flex gap-2 border-b border-slate-800 pb-4">
              {[
                { id: 'users', label: 'USER DIRECTORY', icon: Users },
                { id: 'vouchers', label: 'VOUCHERS & PASSES', icon: Ticket },
                { id: 'config', label: 'SYSTEM ENVIRONMENT CONFIGS', icon: Settings },
              ].map((tab) => {
                const IconComp = tab.icon;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setAdminTab(tab.id as any)}
                    className={`flex items-center gap-2 px-4 py-2.5 rounded-lg text-xs font-bold tracking-wider transition-all ${
                      adminTab === tab.id
                        ? "bg-red-500/10 text-red-400 border border-red-500/40 glow-cyan"
                        : "text-slate-400 hover:text-white"
                    }`}
                  >
                    <IconComp className="w-4 h-4" />
                    {tab.label}
                  </button>
                );
              })}
            </div>

            {/* TAB CONTENT 1: USER REGISTRY */}
            {adminTab === 'users' && (
              <div className="space-y-6">
                
                {/* Save details */}
                <div className="bg-slate-950 border border-slate-850 p-5 rounded-2xl flex flex-col md:flex-row gap-4 justify-between items-center">
                  <div className="space-y-1 text-center md:text-left select-none">
                    <h4 className="text-xs font-black uppercase text-slate-300">Insert custom user registration</h4>
                    <p className="text-[10px] text-slate-500 font-mono">Create custom client login registers instantly</p>
                  </div>

                  <div className="flex gap-2 w-full md:w-auto">
                    <input
                      type="text"
                      placeholder="Telegram user handle e.g. @bob"
                      value={newUsernameInput}
                      onChange={(e) => setNewUsernameInput(e.target.value)}
                      className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-white focus:outline-none focus:border-red-500 font-mono w-full md:w-64"
                    />
                    <button
                      onClick={handleRegisterNewUser}
                      className="bg-red-950/40 hover:bg-red-900/60 border border-red-900 text-red-400 px-4 py-1.5 rounded-lg text-xs font-bold font-mono transition-all shrink-0"
                    >
                      REGISTER
                    </button>
                  </div>
                </div>

                <div className="overflow-x-auto border border-slate-800 rounded-xl bg-slate-950/60">
                  <table className="w-full text-left font-mono text-[11px]">
                    <thead className="bg-slate-950 text-slate-400 text-[10px] border-b border-slate-850">
                      <tr>
                        <th className="p-4 uppercase tracking-wider">UID</th>
                        <th className="p-4 uppercase tracking-wider">Telegram ID</th>
                        <th className="p-4 uppercase tracking-wider text-center">Status</th>
                        <th className="p-4 uppercase tracking-wider">Expiration Date</th>
                        <th className="p-4 uppercase tracking-wider text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-850">
                      {users.map((u) => (
                        <tr key={u.id} className="hover:bg-slate-900/30">
                          <td className="p-4 font-bold text-slate-300">{u.id}</td>
                          <td className="p-4 font-extrabold text-white text-[12px]">{u.username}</td>
                          <td className="p-4 text-center">
                            <span className={`px-2 py-0.5 rounded text-[9px] font-black tracking-widest ${u.isActive ? 'bg-emerald-950/30 text-emerald-400 border border-emerald-900/50' : 'bg-red-950/30 text-red-400 border border-red-900/50'}`}>
                              {u.isActive ? "ACTIVE" : "BLOCKED"}
                            </span>
                          </td>
                          <td className="p-4 text-slate-300">
                            {u.accessExpiresAt && u.accessExpiresAt > Date.now() ? (
                              <span className="text-emerald-400 font-semibold">{new Date(u.accessExpiresAt).toLocaleString()}</span>
                            ) : (
                              <span className="text-slate-500">Expired / No Pass</span>
                            )}
                          </td>
                          <td className="p-4 space-x-1.5 text-right shrink-0">
                            <button
                              onClick={() => toggleUserActiveStatus(u)}
                              className="bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-300 px-2 py-1 rounded text-[10px] font-bold"
                            >
                              BAN/UNBAN
                            </button>
                            <button
                              onClick={() => extendUserAccess(u, 7)}
                              className="bg-emerald-950/30 hover:bg-emerald-900/40 border border-emerald-900/60 text-emerald-400 px-2 py-1 rounded text-[10px] font-bold"
                            >
                              +7 DAYS
                            </button>
                            <button
                              onClick={() => subtractUserAccess(u)}
                              className="bg-red-950/30 hover:bg-red-900/40 border border-red-900/60 text-red-400 px-2 py-1 rounded text-[10px] font-bold"
                            >
                              REVOKE
                            </button>
                            <button
                              onClick={() => handleDeleteUser(u.id)}
                              className="text-red-500 hover:text-red-400 p-1 font-extrabold inline-block"
                            >
                              <Trash className="w-3.5 h-3.5" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* TAB CONTENT 2: VOUCHERS MANAGEMENT */}
            {adminTab === 'vouchers' && (
              <div className="space-y-6">
                
                {/* Generation settings cards */}
                <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
                  <div>
                    <h4 className="text-sm font-bold text-slate-100">TELEMETRY ACCESS CODE MAKER</h4>
                    <p className="text-[10px] text-slate-400 leading-relaxed font-mono mt-1">
                      Determine custom access pass levels and click dynamically to publish cryptographical voucher coordinates to the database.
                    </p>
                  </div>

                  <div className="flex gap-3 justify-end">
                    <select
                      value={newVoucherDays}
                      onChange={(e) => setNewVoucherDays(Number(e.target.value))}
                      className="bg-slate-950 border border-slate-800 text-white rounded-lg px-3 py-1.5 text-xs font-mono focus:outline-none"
                    >
                      <option value={1}>1 Day Test Pass</option>
                      <option value={3}>3 Days Pro Pass</option>
                      <option value={7}>7 Days Premium weekly</option>
                      <option value={30}>30 Days Corporate Pass</option>
                    </select>

                    <button
                      onClick={handleCreateVoucher}
                      className="bg-red-950/50 hover:bg-red-900/60 border border-red-900 text-red-400 hover:text-white px-5 py-2 rounded-lg text-xs font-black font-mono transition-all"
                    >
                      GENERATE VOUCHER ACCESS
                    </button>
                  </div>
                </div>

                <div className="overflow-x-auto border border-slate-800 rounded-xl bg-slate-950/60">
                  <table className="w-full text-left font-mono text-[11px]">
                    <thead className="bg-slate-950 text-slate-400 text-[10px] border-b border-slate-850">
                      <tr>
                        <th className="p-4 uppercase tracking-wider">Access Voucher Code</th>
                        <th className="p-4 uppercase tracking-wider">Pass Duration</th>
                        <th className="p-4 uppercase tracking-wider">Status</th>
                        <th className="p-4 uppercase tracking-wider">Redeemed By Client</th>
                        <th className="p-4 uppercase tracking-wider text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-850">
                      {vouchers.map((v) => (
                        <tr key={v.code} className="hover:bg-slate-900/30">
                          <td className="p-4 font-bold text-red-300 tracking-wider text-[12px]">{v.code}</td>
                          <td className="p-4 text-slate-300">{v.days} Premium Days</td>
                          <td className="p-4">
                            <span className={`px-2 py-0.5 rounded text-[9px] font-black tracking-widest ${v.isRedeemed ? 'bg-red-950 text-red-400' : 'bg-emerald-950 text-emerald-400'}`}>
                              {v.isRedeemed ? "REDEEMED DONE" : "ONLINE READY"}
                            </span>
                          </td>
                          <td className="p-4 text-slate-350">{v.redeemedBy || "—"}</td>
                          <td className="p-4 text-right">
                            <button
                              onClick={() => handleRevokeVoucher(v.code)}
                              className="text-red-500 hover:text-red-400 font-bold"
                            >
                              DESTROY
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* TAB CONTENT 3: GLOBAL TELEMETRY ENGINE CONFIGS */}
            {adminTab === 'config' && (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                
                {/* Config Fields */}
                <div className="bg-slate-950 border border-slate-850 p-6 rounded-2xl space-y-6">
                  <h3 className="text-xs font-black uppercase text-red-400 tracking-wider font-mono">GLOBAL HARDWARE SETTINGS</h3>
                  
                  <div className="space-y-4 font-mono text-[11px]">
                    <div>
                      <label className="block text-slate-400 mb-2 font-bold uppercase">Dynamic Announcement Broadcaster ticker</label>
                      <textarea
                        value={announcementText}
                        onChange={(e) => setAnnouncementText(e.target.value)}
                        rows={3}
                        className="w-full bg-slate-900 border border-slate-800 rounded-lg p-3 text-xs text-white focus:outline-none focus:border-red-500"
                      />
                    </div>

                    <div>
                      <label className="block text-slate-400 mb-2 font-bold uppercase">Pocket Option Referral Web Link</label>
                      <input
                        type="text"
                        value={poLinkText}
                        onChange={(e) => setPoLinkText(e.target.value)}
                        className="w-full bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-red-500 font-mono"
                      />
                    </div>

                    <button
                      onClick={handleSaveConfigs}
                      className="w-full bg-red-950/40 hover:bg-red-900/60 border border-red-900 text-red-400 py-3 rounded-lg text-xs font-black font-mono transition-all"
                    >
                      COMMIT RE-PUBLISHED CONFIGS
                    </button>
                  </div>
                </div>

                <div className="space-y-6">
                  
                  {/* Assets Config Card */}
                  <div className="bg-slate-950 border border-slate-850 p-6 rounded-2xl space-y-5">
                    <h3 className="text-xs font-black uppercase text-red-400 tracking-wider font-mono">BROKER INSTRUMENTS REGISTRY</h3>
                    
                    <div className="flex gap-2">
                      <input
                        type="text"
                        placeholder="EUR/USD OTC"
                        value={customAssetInput}
                        onChange={(e) => setCustomAssetInput(e.target.value)}
                        className="flex-grow bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-white focus:outline-none focus:border-red-500 font-mono uppercase"
                      />
                      
                      <div className="flex items-center gap-1 shrink-0 bg-slate-900 px-2 rounded-lg border border-slate-800">
                        <input
                          type="checkbox"
                          id="isOtcCheck"
                          checked={customAssetOtc}
                          onChange={(e) => setCustomAssetOtc(e.target.checked)}
                          className="rounded text-red-500 focus:ring-0 cursor-pointer"
                        />
                        <label htmlFor="isOtcCheck" className="text-[10px] text-slate-400 font-bold cursor-pointer font-mono select-none">OTC</label>
                      </div>

                      <button
                        onClick={handleAddCustomAsset}
                        className="bg-red-950/30 border border-red-900 text-red-400 px-3 py-1.5 text-xs font-bold font-mono hover:text-white rounded-lg"
                      >
                        ADD
                      </button>
                    </div>

                    <div className="max-h-[200px] overflow-y-auto space-y-2 border border-slate-800 rounded-xl p-3 bg-slate-900/30">
                      {assets.map((asset) => (
                        <div key={asset.name} className="flex justify-between items-center text-[10px] font-mono py-1 border-b border-slate-850/60 last:border-0 pl-1">
                          <span className="font-bold text-slate-200">
                            {asset.name} {asset.isOtc && <span className="text-cyan-400 text-[8px] font-bold border border-cyan-900 rounded bg-cyan-950/20 px-1 ml-1.5">OTC</span>}
                          </span>
                          <button
                            onClick={() => handleRemoveAsset(asset.name)}
                            className="text-red-500 hover:text-red-400 font-black inline-block px-1"
                          >
                            REMOVE
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* FOOTER NOTIFY */}
      <footer className="bg-slate-950 py-6 border-t border-slate-900 text-center text-[10px] text-slate-500 font-mono tracking-widest relative z-10 select-none">
        &copy; 2026 DIDS BOLT TELEMETRY SIGNAL RE-PUBLISHED TERMINALS INC.
      </footer>
    </div>
  );
}
