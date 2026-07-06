import {
  ArrowLeft,
  CalendarDays,
  Check,
  ChefHat,
  Clock,
  ClipboardList,
  Edit3,
  Eye,
  LogIn,
  LogOut,
  Mail,
  MessageSquareText,
  Plus,
  RefreshCw,
  Save,
  Settings,
  ShieldAlert,
  Sparkles,
  Star,
  TicketCheck,
  User,
  Users,
  Utensils,
} from 'lucide-react';
import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import { api } from './api/client';
import {
  ApiError,
  AuthResult,
  Booking,
  BookingStatus,
  ChefResponse,
  CurrentUser,
  EquipmentChoice,
  ParticipantBooking,
  Program,
  ProgramInput,
  Review,
  SlotDetails,
  SlotSummary,
  StudioSettings,
  UserRole,
} from './types';

const TOKEN_KEY = 'chef-stol-token';
const USER_KEY = 'chef-stol-user';
const EMAIL_KEY = 'chef-stol-email';
const RETURN_TO_KEY = 'chef-stol-return-to';

function routePath() {
  return `${window.location.pathname}${window.location.search}`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function formatDay(value: string) {
  return new Intl.DateTimeFormat('ru-RU', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date(value));
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('ru-RU', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function formatDateInput(date: Date) {
  return date.toISOString().slice(0, 10);
}

function apiMessage(error: unknown) {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Не удалось выполнить действие';
}

function isLateCancellation(booking: Booking) {
  const startAt = new Date(booking.slot.startAt).getTime();
  const now = Date.now();
  return startAt - now < 6 * 60 * 60 * 1000;
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    SCHEDULED: 'Запланирован',
    CANCELLED_BY_STUDIO: 'Отменено студией',
    COMPLETED: 'Завершён',
    CONFIRMED: 'Подтверждена',
    CANCELLED_BY_CLIENT: 'Отменена вами',
    NO_SHOW: 'Неявка',
  };
  return labels[status] ?? status;
}

function participantStatusLabel(status: BookingStatus) {
  const labels: Record<BookingStatus, string> = {
    CONFIRMED: 'Подтверждена',
    CANCELLED_BY_CLIENT: 'Отменена клиентом',
    CANCELLED_BY_STUDIO: 'Отменена студией',
    COMPLETED: 'Завершена',
    NO_SHOW: 'Неявка',
  };
  return labels[status] ?? status;
}

function pluralizeRu(count: number, one: string, few: string, many: string) {
  const mod10 = count % 10;
  const mod100 = count % 100;
  if (mod10 === 1 && mod100 !== 11) return one;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
  return many;
}

function className(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(' ');
}

function roleHome(user: CurrentUser) {
  if (user.role === 'ADMIN') return '/admin/schedule';
  if (user.role === 'CHEF') return '/chef/classes';
  return '/';
}

function requireToken(token: string | null, navigate: (to: string) => void, returnTo: string) {
  if (!token) {
    localStorage.setItem(RETURN_TO_KEY, returnTo);
    navigate('/auth/email');
    return null;
  }
  return token;
}

const roleLabels: Record<UserRole, string> = {
  CLIENT: 'Клиент',
  CHEF: 'Шеф',
  ADMIN: 'Админ',
};

function clearAuthAndRelogin() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.assign('/auth/email');
}

function RoleGuard({
  user,
  expected,
  children,
}: {
  user: CurrentUser | null;
  expected: UserRole;
  children: ReactNode;
}) {
  if (!user) {
    return (
      <section className="screen">
        <StateBlock
          kind="empty"
          text="Для этого раздела нужно войти под нужной ролью"
          actionLabel="Войти"
          onAction={clearAuthAndRelogin}
        />
      </section>
    );
  }

  if (user.role !== expected) {
    return (
      <section className="screen">
        <StateBlock
          kind="error"
          text={`Сейчас активна роль: ${roleLabels[user.role]}. Для этого раздела нужна роль: ${roleLabels[expected]}.`}
          actionLabel="Войти заново"
          onAction={clearAuthAndRelogin}
        />
      </section>
    );
  }

  return <>{children}</>;
}

export function App() {
  const [path, setPath] = useState(routePath());
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<CurrentUser | null>(() => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) as CurrentUser : null;
  });

  const navigate = useCallback((to: string) => {
    window.history.pushState({}, '', to);
    setPath(routePath());
  }, []);

  useEffect(() => {
    const onPop = () => setPath(routePath());
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, []);

  useEffect(() => {
    if (!token) {
      return;
    }
    api.me(token)
      .then((nextUser) => {
        setUser(nextUser);
        localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
      })
      .catch(() => {
        setToken(null);
        setUser(null);
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
      });
  }, [token]);

  const completeAuth = useCallback((result: AuthResult, nextUser = result.user) => {
    localStorage.setItem(TOKEN_KEY, result.accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setToken(result.accessToken);
    setUser(nextUser);
  }, []);

  const logout = useCallback(() => {
    api.logout(token).catch(() => undefined);
    setToken(null);
    setUser(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    navigate('/');
  }, [navigate, token]);

  const content = useMemo(() => {
    const [pathname] = path.split('?');
    if (pathname === '/auth/email') {
      return <AuthEmailPage navigate={navigate} />;
    }
    if (pathname === '/auth/code') {
      return <AuthCodePage completeAuth={completeAuth} navigate={navigate} />;
    }
    if (pathname === '/me/bookings') {
      return <MyBookingsPage token={token} navigate={navigate} />;
    }
    if (pathname === '/chef/classes') {
      return <RoleGuard user={user} expected="CHEF"><ChefClassesPage token={token} navigate={navigate} /></RoleGuard>;
    }
    if (pathname.startsWith('/chef/classes/')) {
      return (
        <RoleGuard user={user} expected="CHEF">
          <ParticipantsPage slotId={pathname.replace('/chef/classes/', '')} token={token} navigate={navigate} mode="chef" />
        </RoleGuard>
      );
    }
    if (pathname === '/admin/schedule') {
      return <RoleGuard user={user} expected="ADMIN"><AdminSchedulePage token={token} navigate={navigate} /></RoleGuard>;
    }
    if (pathname.startsWith('/admin/slots/') && pathname.endsWith('/participants')) {
      return (
        <RoleGuard user={user} expected="ADMIN">
          <ParticipantsPage slotId={pathname.replace('/admin/slots/', '').replace('/participants', '')} token={token} navigate={navigate} mode="admin" />
        </RoleGuard>
      );
    }
    if (pathname === '/admin/programs') {
      return <RoleGuard user={user} expected="ADMIN"><AdminProgramsPage token={token} navigate={navigate} /></RoleGuard>;
    }
    if (pathname === '/admin/chefs') {
      return <RoleGuard user={user} expected="ADMIN"><AdminChefsPage token={token} navigate={navigate} /></RoleGuard>;
    }
    if (pathname === '/admin/settings') {
      return <RoleGuard user={user} expected="ADMIN"><AdminSettingsPage token={token} navigate={navigate} /></RoleGuard>;
    }
    if (pathname === '/admin/reviews') {
      return <RoleGuard user={user} expected="ADMIN"><AdminReviewsPage token={token} navigate={navigate} /></RoleGuard>;
    }
    if (pathname.startsWith('/slots/')) {
      return <SlotPage slotId={pathname.replace('/slots/', '')} token={token} navigate={navigate} />;
    }
    if (pathname.startsWith('/booking/')) {
      return <BookingPage slotId={pathname.replace('/booking/', '')} token={token} navigate={navigate} />;
    }
    return <SchedulePage navigate={navigate} />;
  }, [completeAuth, navigate, path, token, user]);

  return (
    <div className="app-shell">
      <Header user={user} navigate={navigate} logout={logout} />
      <main>{content}</main>
    </div>
  );
}

function Header({
  user,
  navigate,
  logout,
}: {
  user: CurrentUser | null;
  navigate: (to: string) => void;
  logout: () => void;
}) {
  return (
    <header className="topbar">
      <button className="brand" onClick={() => navigate('/')} type="button">
        <span className="brand-mark">
          <ChefHat size={22} />
        </span>
        <span>
          <strong>Шеф-стол</strong>
          <small>кулинарная студия</small>
        </span>
      </button>

      <nav className="topnav" aria-label="Главная навигация">
        <button onClick={() => navigate('/')} type="button">
          <CalendarDays size={18} />
          Расписание
        </button>
        {user?.role === 'CLIENT' && (
          <button onClick={() => navigate('/me/bookings')} type="button">
            <TicketCheck size={18} />
            Мои брони
          </button>
        )}
        {user?.role === 'CHEF' && (
          <button onClick={() => navigate('/chef/classes')} type="button">
            <ClipboardList size={18} />
            Мои классы
          </button>
        )}
        {user?.role === 'ADMIN' && (
          <>
            <button onClick={() => navigate('/admin/schedule')} type="button">
              <CalendarDays size={18} />
              Расписание
            </button>
            <button onClick={() => navigate('/admin/programs')} type="button">
              <Utensils size={18} />
              Программы
            </button>
            <button onClick={() => navigate('/admin/chefs')} type="button">
              <Users size={18} />
              Шефы
            </button>
            <button onClick={() => navigate('/admin/settings')} type="button">
              <Settings size={18} />
              Настройки
            </button>
            <button onClick={() => navigate('/admin/reviews')} type="button">
              <MessageSquareText size={18} />
              Отзывы
            </button>
          </>
        )}
      </nav>

      <div className="account">
        {user ? (
          <>
            <span className="account-name">
              <User size={16} />
              {user.name || user.email}
            </span>
            <span className={className('role-badge', user.role.toLowerCase())}>{roleLabels[user.role]}</span>
            <button className="icon-button" onClick={logout} title="Выйти" type="button">
              <LogOut size={18} />
            </button>
          </>
        ) : (
          <button className="ghost-button" onClick={() => navigate('/auth/email')} type="button">
            <LogIn size={18} />
            Войти
          </button>
        )}
      </div>
    </header>
  );
}

function SchedulePage({ navigate }: { navigate: (to: string) => void }) {
  const today = formatDateInput(new Date());
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [slots, setSlots] = useState<SlotSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    const params = new URLSearchParams();
    if (dateFrom) params.set('dateFrom', dateFrom);
    if (dateTo) params.set('dateTo', dateTo);
    api.slots(params)
      .then(setSlots)
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  }, [dateFrom, dateTo]);

  useEffect(() => {
    load();
  }, [load]);

  const grouped = useMemo(() => {
    return slots.reduce<Record<string, SlotSummary[]>>((acc, slot) => {
      const key = new Date(slot.startAt).toISOString().slice(0, 10);
      acc[key] = [...(acc[key] ?? []), slot];
      return acc;
    }, {});
  }, [slots]);

  return (
    <section className="screen schedule-screen">
      <div className="screen-heading">
        <div>
          <p className="eyebrow">Афиша недели</p>
          <h1>Выберите вечер за большим столом</h1>
        </div>
        <div className="date-filter" aria-label="Фильтр по датам">
          <label>
            C
            <input type="date" min={today} value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} />
          </label>
          <label>
            По
            <input type="date" min={dateFrom || today} value={dateTo} onChange={(event) => setDateTo(event.target.value)} />
          </label>
          <button className="icon-button" onClick={() => { setDateFrom(''); setDateTo(''); }} title="Сбросить фильтр" type="button">
            <RefreshCw size={18} />
          </button>
        </div>
      </div>

      {loading && <SkeletonList />}
      {!loading && error && <StateBlock kind="error" text={error} actionLabel="Попробовать снова" onAction={load} />}
      {!loading && !error && slots.length === 0 && (
        <StateBlock kind="empty" text="Пока нет доступных классов" actionLabel="Ближайшие 7 дней" onAction={() => { setDateFrom(''); setDateTo(''); }} />
      )}
      {!loading && !error && slots.length > 0 && (
        <div className="day-list">
          {Object.entries(grouped).map(([day, daySlots]) => (
            <section className="day-section" key={day}>
              <h2>{formatDay(day)}</h2>
              <div className="slot-grid">
                {daySlots.map((slot) => (
                  <SlotCard key={slot.id} slot={slot} onOpen={() => navigate(`/slots/${slot.id}`)} />
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </section>
  );
}

function SlotCard({ slot, onOpen }: { slot: SlotSummary; onOpen: () => void }) {
  const full = slot.freeSpots <= 0;
  const cancelled = slot.status === 'CANCELLED_BY_STUDIO';
  return (
    <article className={className('slot-card', full && 'is-full', cancelled && 'is-muted')}>
      <button className="card-button" onClick={onOpen} type="button">
        <span className="food-visual" aria-hidden="true" />
        <span className="card-content">
          <span className="meta-line">
            <Clock size={16} />
            {formatTime(slot.startAt)}
          </span>
          <strong>{slot.programTitle}</strong>
          <span className="chef-line">
            <ChefHat size={16} />
            {slot.chef.name || 'Шеф'}
            {slot.chef.averageRating && (
              <span className="rating">
                <Star size={15} />
                {slot.chef.averageRating.toFixed(1)}
              </span>
            )}
          </span>
          <span className={className('spots', full && 'warning')}>
            {cancelled ? 'Отменён студией' : full ? 'Мест нет' : `${slot.freeSpots} из ${slot.capacityTotal} мест`}
          </span>
        </span>
      </button>
    </article>
  );
}

function SlotPage({ slotId, token, navigate }: { slotId: string; token: string | null; navigate: (to: string) => void }) {
  const [slot, setSlot] = useState<SlotDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    api.slot(slotId)
      .then(setSlot)
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  }, [slotId]);

  if (loading) return <section className="screen"><SkeletonList /></section>;
  if (error || !slot) return <section className="screen"><StateBlock kind="error" text={error || 'Класс не найден'} actionLabel="К расписанию" onAction={() => navigate('/')} /></section>;

  const unavailable = slot.status !== 'SCHEDULED' || slot.freeSpots <= 0;
  const startBooking = () => {
    if (!token) {
      localStorage.setItem(RETURN_TO_KEY, `/booking/${slot.id}`);
      navigate('/auth/email');
      return;
    }
    navigate(`/booking/${slot.id}`);
  };

  return (
    <section className="screen details-screen">
      <button className="back-button" onClick={() => navigate('/')} type="button">
        <ArrowLeft size={18} />
        Расписание
      </button>
      <div className="details-hero">
        <div className="details-copy">
          <p className="eyebrow">{slot.program.cuisineType}</p>
          <h1>{slot.program.title}</h1>
          <p>{slot.program.description}</p>
          <div className="facts-row">
            <span><CalendarDays size={17} />{formatDateTime(slot.startAt)}</span>
            <span><Clock size={17} />{slot.durationMinutes / 60} часа</span>
            <span><Utensils size={17} />{slot.freeSpots} мест</span>
          </div>
          <button className="primary-button" disabled={unavailable} onClick={startBooking} type="button">
            <TicketCheck size={18} />
            {unavailable ? 'Запись недоступна' : 'Забронировать'}
          </button>
        </div>
        <div className="hero-media" aria-hidden="true">
          <span>chef table</span>
        </div>
      </div>
      <div className="details-grid">
        <section>
          <h2>Что готовим</h2>
          <div className="dish-list">
            {slot.program.dishes.length ? slot.program.dishes.map((dish) => <span key={dish}>{dish}</span>) : <span>Меню уточняется</span>}
          </div>
        </section>
        <section>
          <h2>Шеф</h2>
          <p className="chef-bio">{slot.chef.name || 'Шеф студии'}</p>
          {slot.chef.bio && <p>{slot.chef.bio}</p>}
          <p className="rating-row">
            <Star size={17} />
            {slot.chef.averageRating ? `${slot.chef.averageRating.toFixed(1)} · ${slot.chef.reviewsCount} отзывов` : 'Пока без отзывов'}
          </p>
        </section>
        <section>
          <h2>Прокат</h2>
          {slot.rentalSetsAvailable > 0 ? (
            <p>{slot.rentalSetsAvailable} наборов, {slot.rentalPricePerSet ?? 0} ₽ за набор. Оплата на месте.</p>
          ) : (
            <p>Прокатных наборов нет, приходите со своей экипировкой.</p>
          )}
        </section>
      </div>
    </section>
  );
}

function BookingPage({ slotId, token, navigate }: { slotId: string; token: string | null; navigate: (to: string) => void }) {
  const [slot, setSlot] = useState<SlotDetails | null>(null);
  const [choice, setChoice] = useState<EquipmentChoice>('OWN');
  const [booking, setBooking] = useState<Booking | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!token) {
      localStorage.setItem(RETURN_TO_KEY, `/booking/${slotId}`);
      navigate('/auth/email');
      return;
    }
    api.slot(slotId)
      .then((nextSlot) => {
        setSlot(nextSlot);
        if (nextSlot.rentalSetsAvailable <= 0) setChoice('OWN');
      })
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  }, [navigate, slotId, token]);

  const submit = () => {
    if (!token || !slot) return;
    setSubmitting(true);
    setError('');
    api.createBooking(token, slot.id, choice)
      .then(setBooking)
      .catch((err) => {
        if (err instanceof ApiError && err.code === 'CLIENT_BLOCKED') {
          setError(`Бронирование закрыто до ${formatDateTime(err.blockedUntil || '')}`);
        } else {
          setError(apiMessage(err));
        }
      })
      .finally(() => setSubmitting(false));
  };

  if (loading) return <section className="screen"><SkeletonList /></section>;
  if (!slot) return <section className="screen"><StateBlock kind="error" text={error || 'Класс не найден'} actionLabel="К расписанию" onAction={() => navigate('/')} /></section>;
  if (booking) {
    return (
      <section className="screen success-screen">
        <div className="success-mark"><Check size={34} /></div>
        <h1>Место за столом ваше</h1>
        <p>{slot.program.title}, {formatDateTime(slot.startAt)}</p>
        {booking.rentalPriceSnapshot !== null && <p>Прокат зафиксирован: {booking.rentalPriceSnapshot} ₽.</p>}
        <p>Напоминания придут на email за 24 часа и за 2 часа до начала.</p>
        <div className="action-row">
          <button className="primary-button" onClick={() => navigate('/me/bookings')} type="button">Мои брони</button>
          <button className="ghost-button" onClick={() => navigate('/')} type="button">Расписание</button>
        </div>
      </section>
    );
  }

  const rentalDisabled = slot.rentalSetsAvailable <= 0;
  const unavailable = slot.status !== 'SCHEDULED' || slot.freeSpots <= 0;

  return (
    <section className="screen booking-screen">
      <button className="back-button" onClick={() => navigate(`/slots/${slot.id}`)} type="button">
        <ArrowLeft size={18} />
        К классу
      </button>
      <div className="booking-layout">
        <div>
          <p className="eyebrow">Оформление брони</p>
          <h1>{slot.program.title}</h1>
          <p>{formatDateTime(slot.startAt)} · {slot.chef.name || 'Шеф студии'}</p>
          <div className="choice-group" role="radiogroup" aria-label="Выбор экипировки">
            <button className={className('choice-button', choice === 'OWN' && 'is-selected')} onClick={() => setChoice('OWN')} type="button">
              <Utensils size={22} />
              <span>
                <strong>Своя экипировка</strong>
                <small>Без прокатного набора</small>
              </span>
            </button>
            <button className={className('choice-button', choice === 'RENTAL' && 'is-selected')} disabled={rentalDisabled} onClick={() => setChoice('RENTAL')} type="button">
              <Sparkles size={22} />
              <span>
                <strong>Прокат студии</strong>
                <small>{rentalDisabled ? 'Наборы закончились' : `${slot.rentalPricePerSet ?? 0} ₽, цена фиксируется`}</small>
              </span>
            </button>
          </div>
          <p className="soft-note">Онлайн-оплаты нет, расчёт за прокат проходит в студии.</p>
          {error && <div className="inline-error"><ShieldAlert size={18} />{error}</div>}
          <button className="primary-button" disabled={unavailable || submitting} onClick={submit} type="button">
            <TicketCheck size={18} />
            {submitting ? 'Бронируем...' : unavailable ? 'Запись недоступна' : 'Забронировать'}
          </button>
        </div>
        <aside className="booking-summary">
          <span className="food-visual" aria-hidden="true" />
          <strong>{slot.freeSpots} из {slot.capacityTotal} мест</strong>
          <span>{statusLabel(slot.status)}</span>
        </aside>
      </div>
    </section>
  );
}

function AuthEmailPage({ navigate }: { navigate: (to: string) => void }) {
  const [email, setEmail] = useState(localStorage.getItem(EMAIL_KEY) ?? '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    api.requestCode(email)
      .then(() => {
        localStorage.setItem(EMAIL_KEY, email);
        navigate('/auth/code');
      })
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  };

  return (
    <section className="auth-screen">
      <form className="auth-panel" onSubmit={submit}>
        <Mail size={28} />
        <h1>Вход по email</h1>
        <label>
          Email
          <input autoComplete="email" required type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        {error && <div className="inline-error">{error}</div>}
        <button className="primary-button" disabled={loading} type="submit">
          {loading ? 'Отправляем...' : 'Получить код'}
        </button>
      </form>
    </section>
  );
}

function AuthCodePage({ completeAuth, navigate }: { completeAuth: (result: AuthResult, user?: CurrentUser) => void; navigate: (to: string) => void }) {
  const email = localStorage.getItem(EMAIL_KEY) ?? '';
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [pending, setPending] = useState<AuthResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const finish = (result: AuthResult, nextUser = result.user) => {
    completeAuth(result, nextUser);
    const returnTo = localStorage.getItem(RETURN_TO_KEY) || roleHome(nextUser);
    localStorage.removeItem(RETURN_TO_KEY);
    navigate(returnTo);
  };

  const submitCode = (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    api.verifyCode(email, code)
      .then((result) => {
        if (result.isNewUser) {
          setPending(result);
          return;
        }
        finish(result);
      })
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  };

  const submitName = (event: FormEvent) => {
    event.preventDefault();
    if (!pending) return;
    setLoading(true);
    setError('');
    api.updateMe(pending.accessToken, { name })
      .then((nextUser) => finish(pending, nextUser))
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  };

  if (!email) {
    return <section className="screen"><StateBlock kind="empty" text="Email не указан" actionLabel="Ввести email" onAction={() => navigate('/auth/email')} /></section>;
  }

  return (
    <section className="auth-screen">
      {!pending ? (
        <form className="auth-panel" onSubmit={submitCode}>
          <Mail size={28} />
          <h1>Код из письма</h1>
          <p>{email}</p>
          <label>
            Шестизначный код
            <input inputMode="numeric" maxLength={6} pattern="[0-9]{6}" required value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))} />
          </label>
          {error && <div className="inline-error">{error}</div>}
          <button className="primary-button" disabled={loading || code.length !== 6} type="submit">
            {loading ? 'Проверяем...' : 'Войти'}
          </button>
        </form>
      ) : (
        <form className="auth-panel" onSubmit={submitName}>
          <User size={28} />
          <h1>Как к вам обращаться?</h1>
          <label>
            Имя
            <input autoComplete="name" maxLength={100} required value={name} onChange={(event) => setName(event.target.value)} />
          </label>
          {error && <div className="inline-error">{error}</div>}
          <button className="primary-button" disabled={loading || !name.trim()} type="submit">
            Продолжить
          </button>
        </form>
      )}
    </section>
  );
}

function MyBookingsPage({ token, navigate }: { token: string | null; navigate: (to: string) => void }) {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    if (!token) {
      localStorage.setItem(RETURN_TO_KEY, '/me/bookings');
      navigate('/auth/email');
      return;
    }
    setLoading(true);
    setError('');
    api.myBookings(token)
      .then(setBookings)
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  }, [navigate, token]);

  useEffect(() => {
    load();
  }, [load]);

  const cancel = (booking: Booking) => {
    if (!token) return;
    const late = isLateCancellation(booking);
    const confirmed = window.confirm(late
      ? 'Отмена будет считаться поздней. Отменить бронь?'
      : 'Отменить бронь?');
    if (!confirmed) return;
    api.cancelBooking(token, booking.id)
      .then(load)
      .catch((err) => setError(apiMessage(err)));
  };

  return (
    <section className="screen bookings-screen">
      <div className="screen-heading">
        <div>
          <p className="eyebrow">Личный кабинет</p>
          <h1>Мои брони</h1>
        </div>
      </div>
      {loading && <SkeletonList />}
      {!loading && error && <StateBlock kind="error" text={error} actionLabel="Повторить" onAction={load} />}
      {!loading && !error && bookings.length === 0 && <StateBlock kind="empty" text="Броней пока нет" actionLabel="К расписанию" onAction={() => navigate('/')} />}
      {!loading && !error && bookings.length > 0 && (
        <div className="booking-list">
          {bookings.map((booking) => (
            <article className="booking-row" key={booking.id}>
              <div>
                <strong>{booking.slot.programTitle}</strong>
                <span>{formatDateTime(booking.slot.startAt)} · {statusLabel(booking.status)}</span>
                {booking.isLateCancellation && <small>Поздняя отмена</small>}
                {booking.status === 'NO_SHOW' && <small>Неявка считается нарушением</small>}
              </div>
              <div className="row-actions">
                <button className="ghost-button" onClick={() => navigate(`/slots/${booking.slot.id}`)} type="button">Класс</button>
                {booking.status === 'CONFIRMED' && (
                  <button className="danger-button" onClick={() => cancel(booking)} type="button">Отменить</button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function ChefClassesPage({ token, navigate }: { token: string | null; navigate: (to: string) => void }) {
  const [period, setPeriod] = useState<'UPCOMING' | 'PAST'>('UPCOMING');
  const [slots, setSlots] = useState<SlotSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const auth = requireToken(token, navigate, '/chef/classes');
    if (!auth) return;
    setLoading(true);
    setError('');
    api.chefSlots(auth, period)
      .then(setSlots)
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  }, [navigate, period, token]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <section className="screen work-screen">
      <div className="screen-heading">
        <div>
          <p className="eyebrow">Кабинет шефа</p>
          <h1>Мои классы</h1>
        </div>
        <div className="segmented">
          <button className={period === 'UPCOMING' ? 'is-active' : ''} onClick={() => setPeriod('UPCOMING')} type="button">Предстоящие</button>
          <button className={period === 'PAST' ? 'is-active' : ''} onClick={() => setPeriod('PAST')} type="button">Прошедшие</button>
        </div>
      </div>
      {loading && <SkeletonList />}
      {!loading && error && <StateBlock kind="error" text={error} actionLabel="Повторить" onAction={load} />}
      {!loading && !error && slots.length === 0 && <StateBlock kind="empty" text={period === 'UPCOMING' ? 'Пока нет назначенных классов' : 'История классов пока пуста'} actionLabel="Обновить" onAction={load} />}
      {!loading && !error && slots.length > 0 && (
        <div className="ops-grid">
          {slots.map((slot) => (
            <WorkCard key={slot.id} slot={slot} onOpen={() => navigate(`/chef/classes/${slot.id}`)} />
          ))}
        </div>
      )}
    </section>
  );
}

function ParticipantsPage({
  slotId,
  token,
  navigate,
  mode,
}: {
  slotId: string;
  token: string | null;
  navigate: (to: string) => void;
  mode: 'chef' | 'admin';
}) {
  const [slot, setSlot] = useState<SlotDetails | null>(null);
  const [participants, setParticipants] = useState<ParticipantBooking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const auth = requireToken(token, navigate, mode === 'chef' ? `/chef/classes/${slotId}` : `/admin/slots/${slotId}/participants`);
    if (!auth) return;
    setLoading(true);
    setError('');
    Promise.all([api.slot(slotId), api.slotBookings(auth, slotId)])
      .then(([nextSlot, nextParticipants]) => {
        setSlot(nextSlot);
        setParticipants(nextParticipants);
      })
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  }, [mode, navigate, slotId, token]);

  useEffect(() => {
    load();
  }, [load]);

  const markNoShow = (participant: ParticipantBooking) => {
    const auth = requireToken(token, navigate, routePath());
    if (!auth) return;
    const ok = window.confirm(`Отметить неявку: ${participant.clientName || 'участник'}? Это влияет на возможность клиента бронировать дальше.`);
    if (!ok) return;
    api.markNoShow(auth, participant.id).then(load).catch((err) => setError(apiMessage(err)));
  };

  const revokeNoShow = (participant: ParticipantBooking) => {
    const auth = requireToken(token, navigate, routePath());
    if (!auth) return;
    const ok = window.confirm(`Снять ошибочную неявку: ${participant.clientName || 'участник'}?`);
    if (!ok) return;
    api.revokeNoShow(auth, participant.id).then(load).catch((err) => setError(apiMessage(err)));
  };

  const canMark = slot ? new Date(slot.startAt).getTime() <= Date.now() && slot.status !== 'CANCELLED_BY_STUDIO' : false;
  const activeCount = participants.filter((p) => p.status === 'CONFIRMED').length;
  const rentalCount = participants.filter((p) => p.equipmentChoice === 'RENTAL' && p.status === 'CONFIRMED').length;

  return (
    <section className="screen work-screen">
      <button className="back-button" onClick={() => navigate(mode === 'chef' ? '/chef/classes' : '/admin/schedule')} type="button">
        <ArrowLeft size={18} />
        Назад
      </button>
      {loading && <SkeletonList />}
      {!loading && error && <StateBlock kind="error" text={error} actionLabel="Повторить" onAction={load} />}
      {!loading && slot && !error && (
        <>
          <div className="screen-heading">
            <div>
              <p className="eyebrow">{formatDateTime(slot.startAt)}</p>
              <h1>{slot.programTitle}</h1>
              <p className="soft-note">Прокатных наборов подготовить: {rentalCount}</p>
            </div>
            <span className="spots">
              {activeCount} {pluralizeRu(activeCount, 'активный участник', 'активных участника', 'активных участников')}
            </span>
          </div>
          {participants.length === 0 ? (
            <StateBlock kind="empty" text="На этот класс пока никто не записан" actionLabel="Обновить" onAction={load} />
          ) : (
            <div className="participant-list">
              {participants.map((participant) => (
                <article className={className('participant-row', participant.status !== 'CONFIRMED' && 'is-muted')} key={participant.id}>
                  <div className="participant-main">
                    <strong>{participant.clientName || 'Гость'}</strong>
                    <div className="participant-meta">
                      <span>{participant.equipmentChoice === 'RENTAL' ? 'Прокат' : 'Своя экипировка'}</span>
                      <span className="status-chip">{participantStatusLabel(participant.status)}</span>
                    </div>
                    {participant.allergyNote && <small className="allergy">Аллергия: {participant.allergyNote}</small>}
                  </div>
                  <div className="row-actions">
                    {mode === 'chef' && participant.status === 'CONFIRMED' && (
                      <button className="danger-button" disabled={!canMark} onClick={() => markNoShow(participant)} type="button">Не пришёл</button>
                    )}
                    {mode === 'admin' && participant.status === 'NO_SHOW' && (
                      <button className="ghost-button" onClick={() => revokeNoShow(participant)} type="button">Снять NO_SHOW</button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </>
      )}
    </section>
  );
}

function AdminSchedulePage({ token, navigate }: { token: string | null; navigate: (to: string) => void }) {
  const [slots, setSlots] = useState<SlotSummary[]>([]);
  const [programs, setPrograms] = useState<Program[]>([]);
  const [chefs, setChefs] = useState<ChefResponse[]>([]);
  const [form, setForm] = useState({ programId: '', chefId: '', startAt: '', capacityTotal: 12, rentalSetsAvailable: 0, rentalPricePerSet: 0 });
  const [reasonBySlot, setReasonBySlot] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const auth = requireToken(token, navigate, '/admin/schedule');
    if (!auth) return;
    setLoading(true);
    setError('');
    const params = new URLSearchParams();
    Promise.all([api.slots(params, auth), api.programs(), api.chefs(auth)])
      .then(([nextSlots, nextPrograms, nextChefs]) => {
        setSlots(nextSlots);
        setPrograms(nextPrograms);
        setChefs(nextChefs);
        setForm((current) => ({
          ...current,
          programId: current.programId || nextPrograms[0]?.id || '',
          chefId: current.chefId || nextChefs[0]?.id || '',
        }));
      })
      .catch((err) => setError(apiMessage(err)))
      .finally(() => setLoading(false));
  }, [navigate, token]);

  useEffect(() => {
    load();
  }, [load]);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const auth = requireToken(token, navigate, '/admin/schedule');
    if (!auth) return;
    api.createSlot(auth, {
      programId: form.programId,
      chefId: form.chefId,
      startAt: new Date(form.startAt).toISOString(),
      capacityTotal: Number(form.capacityTotal),
      rentalSetsAvailable: Number(form.rentalSetsAvailable),
      rentalPricePerSet: Number(form.rentalSetsAvailable) > 0 ? Number(form.rentalPricePerSet) : null,
    }).then(load).catch((err) => setError(apiMessage(err)));
  };

  const cancel = (slot: SlotSummary) => {
    const auth = requireToken(token, navigate, '/admin/schedule');
    const reason = (reasonBySlot[slot.id] || '').trim();
    if (!auth || !reason) {
      setError('Укажите причину отмены');
      return;
    }
    api.cancelSlot(auth, slot.id, reason).then(load).catch((err) => setError(apiMessage(err)));
  };

  return (
    <section className="screen admin-screen">
      <div className="screen-heading">
        <div>
          <p className="eyebrow">Администратор</p>
          <h1>Расписание слотов</h1>
          <p className="soft-note">Слот нельзя удалить из истории: запланированный слот отменяется с причиной, новый слот создаётся отдельно.</p>
        </div>
      </div>
      {error && <div className="inline-error"><ShieldAlert size={18} />{error}</div>}
      <form className="ops-form" onSubmit={submit}>
        <select value={form.programId} onChange={(e) => setForm({ ...form, programId: e.target.value })}>
          {programs.map((program) => <option key={program.id} value={program.id}>{program.title}</option>)}
        </select>
        <select value={form.chefId} onChange={(e) => setForm({ ...form, chefId: e.target.value })}>
          {chefs.map((chef) => <option key={chef.id} value={chef.id}>{chef.name || chef.email}</option>)}
        </select>
        <input required type="datetime-local" value={form.startAt} onChange={(e) => setForm({ ...form, startAt: e.target.value })} />
        <input min={1} type="number" value={form.capacityTotal} onChange={(e) => setForm({ ...form, capacityTotal: Number(e.target.value) })} />
        <input min={0} type="number" value={form.rentalSetsAvailable} onChange={(e) => setForm({ ...form, rentalSetsAvailable: Number(e.target.value) })} />
        <input min={0} type="number" value={form.rentalPricePerSet} onChange={(e) => setForm({ ...form, rentalPricePerSet: Number(e.target.value) })} />
        <button className="primary-button" type="submit"><Plus size={18} />Создать слот</button>
      </form>
      {loading && <SkeletonList />}
      {!loading && (
        <div className="ops-grid">
          {slots.map((slot) => (
            <article className="ops-card" key={slot.id}>
              <strong>{slot.programTitle}</strong>
              <span>{formatDateTime(slot.startAt)} · {slot.chef.name || 'Шеф'} · {statusLabel(slot.status)}</span>
              <span>{slot.capacityTotal - slot.freeSpots} из {slot.capacityTotal} записано</span>
              <div className="row-actions">
                <button className="ghost-button" onClick={() => navigate(`/admin/slots/${slot.id}/participants`)} type="button"><Eye size={16} />Участники</button>
              </div>
              {slot.status === 'SCHEDULED' && (
                <div className="cancel-box">
                  <input placeholder="Причина отмены" value={reasonBySlot[slot.id] || ''} onChange={(e) => setReasonBySlot({ ...reasonBySlot, [slot.id]: e.target.value })} />
                  <button className="danger-button" onClick={() => cancel(slot)} type="button">Отменить</button>
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function AdminProgramsPage({ token, navigate }: { token: string | null; navigate: (to: string) => void }) {
  const empty: ProgramInput = { title: '', description: '', cuisineType: '', difficultyLevel: 'BEGINNER', requiresComplexEquipment: false, dishes: [], photos: [] };
  const [programs, setPrograms] = useState<Program[]>([]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<ProgramInput>(empty);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api.programs().then(setPrograms).catch((err) => setError(apiMessage(err)));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const edit = (program: Program) => {
    setEditingId(program.id);
    setForm({
      title: program.title,
      description: program.description,
      cuisineType: program.cuisineType,
      difficultyLevel: program.difficultyLevel,
      requiresComplexEquipment: program.requiresComplexEquipment,
      dishes: program.dishes,
      photos: program.photos,
    });
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const auth = requireToken(token, navigate, '/admin/programs');
    if (!auth) return;
    const action = editingId ? api.updateProgram(auth, editingId, form) : api.createProgram(auth, form);
    action.then(() => { setEditingId(null); setForm(empty); load(); }).catch((err) => setError(apiMessage(err)));
  };

  return (
    <CrudPage title="Каталог программ" eyebrow="Администратор" error={error}>
      <p className="soft-note">Удаление программ не включено в MVP: программу можно создать или отредактировать.</p>
      <form className="ops-form wide" onSubmit={submit}>
        <input placeholder="Название" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        <input placeholder="Тип кухни" required value={form.cuisineType} onChange={(e) => setForm({ ...form, cuisineType: e.target.value })} />
        <select value={form.difficultyLevel} onChange={(e) => setForm({ ...form, difficultyLevel: e.target.value as ProgramInput['difficultyLevel'] })}>
          <option value="BEGINNER">Новичок</option>
          <option value="ADVANCED">Продвинутый</option>
        </select>
        <label className="check-line"><input type="checkbox" checked={form.requiresComplexEquipment} onChange={(e) => setForm({ ...form, requiresComplexEquipment: e.target.checked })} />Сложное оборудование</label>
        <input placeholder="Блюда через запятую" value={form.dishes.join(', ')} onChange={(e) => setForm({ ...form, dishes: e.target.value.split(',').map((x) => x.trim()).filter(Boolean) })} />
        <input className="span-2" placeholder="Описание" required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <button className="primary-button" type="submit"><Save size={18} />{editingId ? 'Сохранить' : 'Создать'}</button>
      </form>
      <div className="ops-grid">
        {programs.map((program) => (
          <article className="ops-card" key={program.id}>
            <strong>{program.title}</strong>
            <span>{program.cuisineType} · {program.difficultyLevel === 'BEGINNER' ? 'новичок' : 'продвинутый'}</span>
            {program.requiresComplexEquipment && <span className="spots warning">Группа до 8</span>}
            <button className="ghost-button" onClick={() => edit(program)} type="button"><Edit3 size={16} />Редактировать</button>
          </article>
        ))}
      </div>
    </CrudPage>
  );
}

function AdminChefsPage({ token, navigate }: { token: string | null; navigate: (to: string) => void }) {
  const [chefs, setChefs] = useState<ChefResponse[]>([]);
  const [form, setForm] = useState({ email: '', name: '', photo: '', bio: '' });
  const [editing, setEditing] = useState<ChefResponse | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    const auth = requireToken(token, navigate, '/admin/chefs');
    if (!auth) return;
    api.chefs(auth).then(setChefs).catch((err) => setError(apiMessage(err)));
  }, [navigate, token]);

  useEffect(() => {
    load();
  }, [load]);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const auth = requireToken(token, navigate, '/admin/chefs');
    if (!auth) return;
    const action = editing
      ? api.updateChef(auth, editing.id, { photo: form.photo, bio: form.bio })
      : api.createChef(auth, form);
    action.then(() => { setForm({ email: '', name: '', photo: '', bio: '' }); setEditing(null); load(); }).catch((err) => setError(apiMessage(err)));
  };

  return (
    <CrudPage title="Шефы" eyebrow="Администратор" error={error}>
      <p className="soft-note">Шефы не удаляются из системы: можно добавить шефа и обновить его фото или био.</p>
      <form className="ops-form wide" onSubmit={submit}>
        <input disabled={!!editing} placeholder="Email" required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input disabled={!!editing} placeholder="Имя" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <input placeholder="Фото URL" value={form.photo} onChange={(e) => setForm({ ...form, photo: e.target.value })} />
        <input className="span-2" placeholder="Био" value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })} />
        <button className="primary-button" type="submit"><Save size={18} />{editing ? 'Сохранить' : 'Добавить шефа'}</button>
      </form>
      <div className="ops-grid">
        {chefs.map((chef) => (
          <article className="ops-card" key={chef.id}>
            <strong>{chef.name || 'Шеф'}</strong>
            <span>{chef.email}</span>
            <span>{chef.averageRating ? `${chef.averageRating.toFixed(1)} · ${chef.reviewsCount} отзывов` : 'Отзывов пока нет'}</span>
            <button className="ghost-button" onClick={() => { setEditing(chef); setForm({ email: chef.email, name: chef.name || '', photo: chef.photo || '', bio: chef.bio || '' }); }} type="button"><Edit3 size={16} />Фото и био</button>
          </article>
        ))}
      </div>
    </CrudPage>
  );
}

function AdminSettingsPage({ token, navigate }: { token: string | null; navigate: (to: string) => void }) {
  const [settings, setSettings] = useState<StudioSettings>({ address: '', contactPhone: '', contactEmail: '' });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    api.studioSettings().then(setSettings).catch((err) => setError(apiMessage(err)));
  }, []);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const auth = requireToken(token, navigate, '/admin/settings');
    if (!auth) return;
    api.updateStudioSettings(auth, settings).then((next) => { setSettings(next); setMessage('Настройки сохранены'); }).catch((err) => setError(apiMessage(err)));
  };

  return (
    <CrudPage title="Настройки студии" eyebrow="Администратор" error={error}>
      <form className="ops-form settings-form" onSubmit={submit}>
        <input placeholder="Адрес" required value={settings.address} onChange={(e) => setSettings({ ...settings, address: e.target.value })} />
        <input placeholder="Телефон" required value={settings.contactPhone} onChange={(e) => setSettings({ ...settings, contactPhone: e.target.value })} />
        <input placeholder="Email" required type="email" value={settings.contactEmail} onChange={(e) => setSettings({ ...settings, contactEmail: e.target.value })} />
        <button className="primary-button" type="submit"><Save size={18} />Сохранить</button>
      </form>
      {message && <p className="soft-note">{message}</p>}
    </CrudPage>
  );
}

function AdminReviewsPage({ token, navigate }: { token: string | null; navigate: (to: string) => void }) {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    const auth = requireToken(token, navigate, '/admin/reviews');
    if (!auth) return;
    api.reviews(auth).then(setReviews).catch((err) => setError(apiMessage(err)));
  }, [navigate, token]);

  return (
    <CrudPage title="Отзывы" eyebrow="Администратор" error={error}>
      <div className="ops-grid">
        {reviews.map((review) => (
          <article className="ops-card" key={review.id}>
            <strong><Star size={16} /> Шеф {review.chefRating} · программа {review.programRating}</strong>
            <span>{formatDateTime(review.createdAt)}</span>
            <p>{review.comment || 'Без комментария'}</p>
          </article>
        ))}
      </div>
    </CrudPage>
  );
}

function WorkCard({ slot, onOpen }: { slot: SlotSummary; onOpen: () => void }) {
  return (
    <article className={className('ops-card', slot.status !== 'SCHEDULED' && 'is-muted')}>
      <strong>{slot.programTitle}</strong>
      <span>{formatDateTime(slot.startAt)} · {statusLabel(slot.status)}</span>
      <span>{slot.capacityTotal - slot.freeSpots} из {slot.capacityTotal} записано</span>
      <button className="ghost-button" onClick={onOpen} type="button"><ClipboardList size={16} />Участники</button>
    </article>
  );
}

function CrudPage({
  title,
  eyebrow,
  error,
  children,
}: {
  title: string;
  eyebrow: string;
  error: string;
  children: ReactNode;
}) {
  return (
    <section className="screen admin-screen">
      <div className="screen-heading">
        <div>
          <p className="eyebrow">{eyebrow}</p>
          <h1>{title}</h1>
        </div>
      </div>
      {error && <div className="inline-error"><ShieldAlert size={18} />{error}</div>}
      {children}
    </section>
  );
}

function SkeletonList() {
  return (
    <div className="skeleton-list" aria-label="Загрузка">
      <span />
      <span />
      <span />
    </div>
  );
}

function StateBlock({
  kind,
  text,
  actionLabel,
  onAction,
}: {
  kind: 'empty' | 'error';
  text: string;
  actionLabel: string;
  onAction: () => void;
}) {
  return (
    <div className={className('state-block', kind === 'error' && 'is-error')}>
      {kind === 'error' ? <ShieldAlert size={28} /> : <Sparkles size={28} />}
      <p>{text}</p>
      <button className="ghost-button" onClick={onAction} type="button">{actionLabel}</button>
    </div>
  );
}
